package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalHistoricalReconciliationFailureDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalHistoricalReconciliationFailureMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = BaseDbUnitTest.Application.class,
        properties = {
                "spring.main.lazy-initialization=true",
                "spring.datasource.url=jdbc:h2:mem:rental-historical-backfill;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:/sql/rental_historical_reconciliation.sql",
                "mybatis.lazy-initialization=true",
                "yudao.info.base-package=cn.iocoder.yudao.module.rental.dal.mysql",
                "rental.historical-backfill.write-enabled=true"
        })
@Import({
        RentalHistoricalOrderBackfillService.class,
        RentalHistoricalOrderBackfillServiceIntegrationTest.ClockConfiguration.class
})
class RentalHistoricalOrderBackfillServiceIntegrationTest {

    private static final long TENANT_ID = 9L;

    @Resource
    private RentalHistoricalOrderBackfillService service;
    @Resource
    private RentalHistoricalReconciliationFailureMapper failureMapper;
    @Resource
    private DataSource dataSource;
    @MockitoBean
    private RentalChannelOrderReconciliationService reconciliationService;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM rental_historical_reconciliation_failure");
        jdbcTemplate.update("DELETE FROM rental_historical_reconciliation_run");
        jdbcTemplate.update("DELETE FROM xianyu_order");
        reset(reconciliationService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void dryRunUsesRealReconciliationButRollsBackBusinessWrites() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        stubConvertedWrites();

        RentalHistoricalBackfillRunResult result = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 2, 100, 1, true, null));

        assertEquals("SUCCEEDED", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(2, result.createdCount());
        assertEquals(2L, result.cursorAfterId());
        assertEquals(0, convertedRows());
    }

    @Test
    void runFreezesRequestedUpperBoundAtCurrentTenantMaximum() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        insertOrder(9_999L, TENANT_ID + 1, null, null);
        stubConvertedWrites();

        RentalHistoricalBackfillRunResult paused = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 10_000, 1, 1, true, null));

        assertEquals("PAUSED", paused.status());
        assertEquals(2L, paused.endIdInclusive());
        assertEquals(1L, paused.cursorAfterId());

        insertOrder(3L, null, null);
        RentalHistoricalBackfillRunResult completed =
                service.resume(paused.runId(), 10, null);

        assertEquals("SUCCEEDED", completed.status());
        assertEquals(2L, completed.endIdInclusive());
        assertEquals(2L, completed.cursorAfterId());
        assertEquals(2, completed.scannedCount());
    }

    @Test
    void emptyCurrentTenantRangeCompletesWithZeroCounts() {
        insertOrder(9_999L, TENANT_ID + 1, null, null);

        RentalHistoricalBackfillRunResult result = service.createAndRun(
                new RentalHistoricalBackfillCommand(100, 10_000, 10, 1, true, null));

        assertEquals("SUCCEEDED", result.status());
        assertEquals(100L, result.endIdInclusive());
        assertEquals(100L, result.cursorAfterId());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.createdCount());
        assertEquals(0, result.failedCount());
    }

    @Test
    void failedBatchRollsBackAndResumeRestartsAtDurableCursor() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        insertOrder(3L, null, null);
        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id == 2L) {
                throw new IllegalStateException("synthetic failure");
            }
            markConverted(id);
            return converted(id, true);
        }).when(reconciliationService).reconcile(anyLong());

        RentalHistoricalBackfillRunResult failed = service.createAndRun(
                new RentalHistoricalBackfillCommand(
                        0, 3, 3, 1, false,
                        RentalHistoricalOrderBackfillService.WRITE_CONFIRMATION));

        assertEquals("FAILED", failed.status());
        assertEquals(0L, failed.cursorAfterId());
        assertEquals(1, failed.failedCount());
        assertEquals(2L, failed.lastFailedOrderId());
        assertEquals(0, convertedRows());
        List<RentalHistoricalReconciliationFailureDO> failures =
                failureMapper.selectListByRunId(failed.runId());
        assertEquals(1, failures.size());
        assertEquals(2L, failures.get(0).getChannelOrderId());
        assertEquals(0L, failures.get(0).getCursorBeforeId());

        stubConvertedWrites();
        RentalHistoricalBackfillRunResult resumed = service.resume(
                failed.runId(), 1, RentalHistoricalOrderBackfillService.WRITE_CONFIRMATION);

        assertEquals("SUCCEEDED", resumed.status());
        assertEquals(3L, resumed.cursorAfterId());
        assertEquals(3, resumed.createdCount());
        assertEquals(1, resumed.failedCount());
        assertEquals(3, convertedRows());
    }

    @Test
    void batchLimitPausesAndResumeContinuesFromCheckpoint() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        insertOrder(3L, null, null);
        stubConvertedWrites();

        RentalHistoricalBackfillRunResult paused = service.createAndRun(
                new RentalHistoricalBackfillCommand(
                        0, 3, 2, 1, false,
                        RentalHistoricalOrderBackfillService.WRITE_CONFIRMATION));

        assertEquals("PAUSED", paused.status());
        assertEquals(2L, paused.cursorAfterId());
        assertEquals(2, paused.createdCount());

        RentalHistoricalBackfillRunResult completed = service.resume(
                paused.runId(), 1, RentalHistoricalOrderBackfillService.WRITE_CONFIRMATION);

        assertEquals("SUCCEEDED", completed.status());
        assertEquals(3L, completed.cursorAfterId());
        assertEquals(3, completed.createdCount());
        assertEquals(1, completed.resumeCount());
    }

    @Test
    void fulfilledConflictIsReportedWithoutReversal() {
        insertOrder(1L, 100L, "CONVERTED");
        doAnswer(invocation -> RentalChannelOrderReconciliationResult.reviewRequired(
                77L, "FULFILLMENT_MODEL_CONFLICT"))
                .when(reconciliationService).reconcile(1L);

        RentalHistoricalBackfillRunResult result = service.createAndRun(
                new RentalHistoricalBackfillCommand(
                        0, 1, 10, 1, false,
                        RentalHistoricalOrderBackfillService.WRITE_CONFIRMATION));

        assertEquals("SUCCEEDED", result.status());
        assertEquals(1, result.conflictCount());
        assertEquals(1, result.reviewRequiredCount());
        assertEquals(100L, jdbcTemplate.queryForObject(
                "SELECT rental_order_id FROM xianyu_order WHERE id = 1", Long.class));
        assertEquals("CONVERTED", jdbcTemplate.queryForObject(
                "SELECT conversion_status FROM xianyu_order WHERE id = 1", String.class));
    }

    @Test
    void succeededRunCannotBeResumed() {
        insertOrder(1L, null, null);
        stubConvertedWrites();
        RentalHistoricalBackfillRunResult completed = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 1, 10, 1, true, null));

        assertThrows(RuntimeException.class,
                () -> service.resume(completed.runId(), 1, null));
    }

    @Test
    void realRunRequiresExplicitConfirmationBeforeCreatingCheckpoint() {
        insertOrder(1L, null, null);
        stubConvertedWrites();

        assertThrows(RuntimeException.class, () -> service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 1, 10, 1, false, null)));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rental_historical_reconciliation_run",
                Integer.class));
        assertEquals(0, convertedRows());
    }

    @Test
    void staleRunningLeaseCanBeTakenOverAndResumed() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        stubConvertedWrites();
        RentalHistoricalBackfillRunResult paused = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 2, 1, 1, true, null));

        jdbcTemplate.update("""
                UPDATE rental_historical_reconciliation_run
                   SET status = 'RUNNING',
                       execution_token = 'abandoned-worker',
                       lease_until = TIMESTAMP '2026-08-31 23:00:00',
                       heartbeat_at = TIMESTAMP '2026-08-31 22:59:00'
                 WHERE id = ?
                """, paused.runId());

        RentalHistoricalBackfillRunResult resumed =
                service.resume(paused.runId(), 1, null);

        assertEquals("SUCCEEDED", resumed.status());
        assertEquals(2L, resumed.cursorAfterId());
        assertEquals(2, resumed.scannedCount());
        assertEquals(1, resumed.resumeCount());
        assertNull(jdbcTemplate.queryForObject("""
                SELECT execution_token
                  FROM rental_historical_reconciliation_run
                 WHERE id = ?
                """, String.class, paused.runId()));
    }

    @Test
    void activeRunningLeaseCannotBeTakenOver() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        stubConvertedWrites();
        RentalHistoricalBackfillRunResult paused = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 2, 1, 1, true, null));

        jdbcTemplate.update("""
                UPDATE rental_historical_reconciliation_run
                   SET status = 'RUNNING',
                       execution_token = 'active-worker',
                       lease_until = TIMESTAMP '2026-09-01 00:35:00',
                       heartbeat_at = TIMESTAMP '2026-09-01 00:30:00'
                 WHERE id = ?
                """, paused.runId());

        assertThrows(RuntimeException.class,
                () -> service.resume(paused.runId(), 1, null));
        assertEquals("active-worker", jdbcTemplate.queryForObject("""
                SELECT execution_token
                  FROM rental_historical_reconciliation_run
                 WHERE id = ?
                """, String.class, paused.runId()));
    }

    @Test
    void candidateQueryFailureMarksRunRecoverable() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        stubConvertedWrites();
        RentalHistoricalBackfillRunResult paused = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 2, 1, 1, true, null));

        jdbcTemplate.execute("ALTER TABLE xianyu_order RENAME TO xianyu_order_unavailable");
        RentalHistoricalBackfillRunResult failed;
        try {
            failed = service.resume(paused.runId(), 1, null);
        } finally {
            jdbcTemplate.execute(
                    "ALTER TABLE xianyu_order_unavailable RENAME TO xianyu_order");
        }

        assertEquals("FAILED", failed.status());
        assertEquals(1L, failed.cursorAfterId());
        assertEquals(1, failed.failedCount());
        assertNull(failed.lastFailedOrderId());

        RentalHistoricalBackfillRunResult resumed =
                service.resume(paused.runId(), 1, null);
        assertEquals("SUCCEEDED", resumed.status());
        assertEquals(2L, resumed.cursorAfterId());
        assertEquals(2, resumed.scannedCount());
    }

    @Test
    void dryRunPauseBetweenRollbackAndCheckpointReturnsPaused() {
        insertOrder(1L, null, null);
        insertOrder(2L, null, null);
        AtomicBoolean pauseRegistered = new AtomicBoolean();
        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            markConverted(id);
            if (pauseRegistered.compareAndSet(false, true)) {
                Long runId = jdbcTemplate.queryForObject(
                        "SELECT MAX(id) FROM rental_historical_reconciliation_run",
                        Long.class);
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                runInTenantThread(() -> service.pause(runId));
                            }
                        });
            }
            return converted(id, true);
        }).when(reconciliationService).reconcile(anyLong());

        RentalHistoricalBackfillRunResult paused = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 2, 1, 2, true, null));

        assertEquals("PAUSED", paused.status());
        assertEquals(1L, paused.cursorAfterId());
        assertEquals(1, paused.scannedCount());
        verify(reconciliationService, times(1)).reconcile(anyLong());
    }

    @Test
    void dryRunCheckpointFailureMarksRunRecoverable() {
        insertOrder(1L, null, null);
        AtomicBoolean constraintRegistered = new AtomicBoolean();
        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            markConverted(id);
            if (constraintRegistered.compareAndSet(false, true)) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                runInThread(() -> jdbcTemplate.execute("""
                                        ALTER TABLE rental_historical_reconciliation_run
                                        ADD CONSTRAINT reject_scanned_checkpoint
                                        CHECK (scanned_count = 0)
                                        """));
                            }
                        });
            }
            return converted(id, true);
        }).when(reconciliationService).reconcile(anyLong());

        RentalHistoricalBackfillRunResult failed = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 1, 1, 1, true, null));

        assertEquals("FAILED", failed.status());
        assertEquals(0L, failed.cursorAfterId());
        assertEquals(0, failed.scannedCount());
        assertEquals(1, failed.failedCount());
        jdbcTemplate.execute("""
                ALTER TABLE rental_historical_reconciliation_run
                DROP CONSTRAINT reject_scanned_checkpoint
                """);

        RentalHistoricalBackfillRunResult resumed =
                service.resume(failed.runId(), 1, null);
        assertEquals("SUCCEEDED", resumed.status());
        assertEquals(1L, resumed.cursorAfterId());
        assertEquals(1, resumed.scannedCount());
    }

    @Test
    void serviceExceptionPersistsSafeDomainCode() {
        insertOrder(1L, null, null);
        doAnswer(invocation -> {
            throw new ServiceException(123456, "sensitive diagnostic");
        }).when(reconciliationService).reconcile(1L);

        RentalHistoricalBackfillRunResult failed = service.createAndRun(
                new RentalHistoricalBackfillCommand(0, 1, 1, 1, true, null));

        assertEquals("FAILED", failed.status());
        assertEquals("SERVICE_123456", failed.lastErrorCode());
        assertEquals("SERVICE_123456",
                failureMapper.selectListByRunId(failed.runId()).get(0).getErrorCode());
    }

    private void stubConvertedWrites() {
        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            markConverted(id);
            return converted(id, true);
        }).when(reconciliationService).reconcile(anyLong());
    }

    private RentalChannelOrderReconciliationResult converted(Long id, boolean planApplied) {
        return RentalChannelOrderReconciliationResult.converted(
                id + 1000,
                new RentalOrderPreparationDecision("READY", null),
                planApplied,
                "CREATED");
    }

    private void markConverted(Long id) {
        jdbcTemplate.update("""
                UPDATE xianyu_order
                   SET rental_order_id = ?, conversion_status = 'CONVERTED',
                       preparation_status = 'READY'
                 WHERE id = ?
                """, id + 1000, id);
    }

    private void insertOrder(Long id, Long rentalOrderId, String conversionStatus) {
        insertOrder(id, TENANT_ID, rentalOrderId, conversionStatus);
    }

    private void insertOrder(
            Long id, Long tenantId, Long rentalOrderId, String conversionStatus) {
        jdbcTemplate.update("""
                INSERT INTO xianyu_order
                    (id, tenant_id, rental_order_id, conversion_status,
                     preparation_status, updater, deleted)
                VALUES (?, ?, ?, ?, NULL, '', FALSE)
                """, id, tenantId, rentalOrderId, conversionStatus);
    }

    private int convertedRows() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM xianyu_order WHERE conversion_status = 'CONVERTED'",
                Integer.class);
    }

    private void runInTenantThread(Runnable action) {
        runInThread(() -> {
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                action.run();
            } finally {
                TenantContextHolder.clear();
            }
        });
    }

    private void runInThread(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        thread.start();
        try {
            thread.join(5_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("test thread interrupted", exception);
        }
        if (thread.isAlive()) {
            thread.interrupt();
            throw new IllegalStateException("test thread timed out");
        }
        Throwable throwable = failure.get();
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable != null) {
            throw new IllegalStateException("test thread failed", throwable);
        }
    }

    @TestConfiguration
    static class ClockConfiguration {

        @Bean("xianyuClock")
        Clock xianyuClock() {
            return Clock.fixed(
                    Instant.parse("2026-08-31T16:30:00Z"),
                    ZoneId.of("Asia/Shanghai"));
        }
    }

}
