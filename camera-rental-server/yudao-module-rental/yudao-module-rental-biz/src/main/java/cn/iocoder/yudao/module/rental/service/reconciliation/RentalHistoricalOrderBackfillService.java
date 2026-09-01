package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalHistoricalReconciliationFailureDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalHistoricalReconciliationRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalHistoricalReconciliationFailureMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalHistoricalReconciliationRunMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_HISTORICAL_BACKFILL_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_HISTORICAL_BACKFILL_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_HISTORICAL_BACKFILL_STATE_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_HISTORICAL_BACKFILL_WRITE_DISABLED;

@Service
public class RentalHistoricalOrderBackfillService {

    public static final String WRITE_CONFIRMATION = "EXECUTE_HISTORICAL_RECONCILIATION";

    private static final String SYSTEM_OPERATOR = "system";
    private static final int MAX_BATCH_SIZE = 500;
    private static final int MAX_BATCHES_PER_CALL = 100;
    private static final Duration EXECUTION_LEASE = Duration.ofMinutes(5);

    private final RentalHistoricalReconciliationRunMapper runMapper;
    private final RentalHistoricalReconciliationFailureMapper failureMapper;
    private final XianyuOrderMapper orderMapper;
    private final RentalChannelOrderReconciliationService reconciliationService;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final boolean writeEnabled;

    @Autowired
    public RentalHistoricalOrderBackfillService(
            RentalHistoricalReconciliationRunMapper runMapper,
            RentalHistoricalReconciliationFailureMapper failureMapper,
            XianyuOrderMapper orderMapper,
            RentalChannelOrderReconciliationService reconciliationService,
            PlatformTransactionManager transactionManager,
            @Qualifier("xianyuClock") Clock clock,
            @Value("${rental.historical-backfill.write-enabled:false}") boolean writeEnabled) {
        this(runMapper, failureMapper, orderMapper, reconciliationService,
                new TransactionTemplate(transactionManager), clock, writeEnabled);
    }

    RentalHistoricalOrderBackfillService(
            RentalHistoricalReconciliationRunMapper runMapper,
            RentalHistoricalReconciliationFailureMapper failureMapper,
            XianyuOrderMapper orderMapper,
            RentalChannelOrderReconciliationService reconciliationService,
            TransactionOperations transactions,
            Clock clock,
            boolean writeEnabled) {
        this.runMapper = runMapper;
        this.failureMapper = failureMapper;
        this.orderMapper = orderMapper;
        this.reconciliationService = reconciliationService;
        this.transactions = transactions;
        this.clock = clock;
        this.writeEnabled = writeEnabled;
    }

    public RentalHistoricalBackfillRunResult createAndRun(RentalHistoricalBackfillCommand command) {
        validateCommand(command);
        requireWriteAuthorization(command.dryRun(), command.confirmation());
        Long runId = required(transactions.execute(status -> createRun(command))).getId();
        return execute(runId, command.maxBatches(), command.confirmation(), false);
    }

    public RentalHistoricalBackfillRunResult resume(
            Long runId, int maxBatches, String confirmation) {
        validateMaxBatches(maxBatches);
        return execute(runId, maxBatches, confirmation, true);
    }

    public RentalHistoricalBackfillRunResult pause(Long runId) {
        return toResult(required(transactions.execute(status -> {
            RentalHistoricalReconciliationRunDO run = requireRunForUpdate(runId);
            if ("SUCCEEDED".equals(run.getStatus())) {
                throw exception(RENTAL_HISTORICAL_BACKFILL_STATE_INVALID, run.getStatus());
            }
            LocalDateTime now = LocalDateTime.now(clock);
            if (isActiveExecution(run, now)) {
                run.setStatus("PAUSE_REQUESTED");
            } else {
                run.setStatus("PAUSED");
                run.setPausedAt(now);
                clearExecution(run);
            }
            run.setUpdater(SYSTEM_OPERATOR);
            runMapper.updateById(run);
            return run;
        })));
    }

    public RentalHistoricalBackfillRunResult get(Long runId) {
        RentalHistoricalReconciliationRunDO run = runMapper.selectById(runId);
        if (run == null) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_NOT_EXISTS);
        }
        return toResult(run);
    }

    private RentalHistoricalBackfillRunResult execute(
            Long runId, int maxBatches, String confirmation, boolean resumed) {
        String executionToken = UUID.randomUUID().toString();
        RentalHistoricalReconciliationRunDO started = required(transactions.execute(status -> {
            RentalHistoricalReconciliationRunDO run = requireRunForUpdate(runId);
            LocalDateTime now = LocalDateTime.now(clock);
            requireRunnable(run, now);
            requireWriteAuthorization(Boolean.TRUE.equals(run.getDryRun()), confirmation);
            run.setStatus("RUNNING");
            run.setExecutionToken(executionToken);
            run.setHeartbeatAt(now);
            run.setLeaseUntil(now.plus(EXECUTION_LEASE));
            run.setPausedAt(null);
            run.setFinishedAt(null);
            run.setLastFailedOrderId(null);
            run.setLastErrorCode(null);
            if (run.getStartedAt() == null) {
                run.setStartedAt(now);
            }
            if (resumed) {
                run.setResumeCount(value(run.getResumeCount()) + 1);
            }
            run.setUpdater(SYSTEM_OPERATOR);
            runMapper.updateById(run);
            return run;
        }));

        for (int batch = 0; batch < maxBatches; batch++) {
            BatchOutcome outcome;
            try {
                outcome = required(transactions.execute(
                        status -> executeBatch(runId, executionToken, status)));
            } catch (BatchExecutionException exception) {
                return recordFailure(runId, executionToken,
                        exception.channelOrderId(), exception.cursorBeforeId(), exception.getCause());
            } catch (ExecutionLeaseLostException exception) {
                return get(runId);
            } catch (RuntimeException exception) {
                return recordFailure(runId, executionToken, null, null, exception);
            }
            if (outcome.paused()) {
                return get(runId);
            }
            if (Boolean.TRUE.equals(started.getDryRun())) {
                RentalHistoricalReconciliationRunDO persisted;
                try {
                    persisted = required(transactions.execute(
                            status -> persistDryRunOutcome(runId, executionToken, outcome)));
                } catch (ExecutionLeaseLostException exception) {
                    return get(runId);
                } catch (RuntimeException exception) {
                    return recordFailure(runId, executionToken, null,
                            outcome.cursorBeforeId(), exception);
                }
                if (!isCurrentExecution(persisted, executionToken)) {
                    return toResult(persisted);
                }
            }
            if (outcome.complete()) {
                return get(runId);
            }
        }
        try {
            return toResult(required(transactions.execute(
                    status -> pauseAtBatchLimit(runId, executionToken))));
        } catch (ExecutionLeaseLostException exception) {
            return get(runId);
        } catch (RuntimeException exception) {
            return recordFailure(runId, executionToken, null, null, exception);
        }
    }

    private BatchOutcome executeBatch(Long runId, String executionToken,
                                      org.springframework.transaction.TransactionStatus transactionStatus) {
        RentalHistoricalReconciliationRunDO run =
                requireOwnedRunForUpdate(runId, executionToken);
        if ("PAUSE_REQUESTED".equals(run.getStatus())) {
            pauseRun(run);
            return BatchOutcome.pausedOutcome();
        }
        if (!"RUNNING".equals(run.getStatus())) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_STATE_INVALID, run.getStatus());
        }
        renewExecution(run);
        long cursor = run.getCursorAfterId();
        if (cursor >= run.getEndIdInclusive()) {
            BatchOutcome complete = BatchOutcome.emptyComplete(cursor, cursor);
            if (Boolean.TRUE.equals(run.getDryRun())) {
                transactionStatus.setRollbackOnly();
            } else {
                applyOutcome(run, complete);
            }
            return complete;
        }
        List<Long> orderIds = orderMapper.selectHistoricalReconciliationCandidateIds(
                run.getTenantId(), cursor, run.getEndIdInclusive(), run.getBatchSize());
        if (orderIds == null || orderIds.isEmpty()) {
            BatchOutcome complete = BatchOutcome.emptyComplete(cursor, cursor);
            if (Boolean.TRUE.equals(run.getDryRun())) {
                transactionStatus.setRollbackOnly();
            } else {
                applyOutcome(run, complete);
            }
            return complete;
        }

        MutableCounts counts = new MutableCounts();
        for (Long orderId : orderIds) {
            try {
                RentalChannelOrderReconciliationResult result =
                        reconciliationService.reconcile(orderId);
                counts.add(classify(result));
            } catch (RuntimeException exception) {
                throw new BatchExecutionException(orderId, cursor, exception);
            }
        }
        long nextCursor = orderIds.get(orderIds.size() - 1);
        boolean complete = nextCursor >= run.getEndIdInclusive()
                || orderIds.size() < run.getBatchSize();
        BatchOutcome outcome = counts.toOutcome(cursor, nextCursor, complete);
        if (Boolean.TRUE.equals(run.getDryRun())) {
            transactionStatus.setRollbackOnly();
        } else {
            applyOutcome(run, outcome);
        }
        return outcome;
    }

    private RentalHistoricalReconciliationRunDO persistDryRunOutcome(
            Long runId, String executionToken, BatchOutcome outcome) {
        RentalHistoricalReconciliationRunDO run =
                requireOwnedRunForUpdate(runId, executionToken);
        boolean pauseRequested = "PAUSE_REQUESTED".equals(run.getStatus());
        if (!"RUNNING".equals(run.getStatus()) && !pauseRequested) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_STATE_INVALID, run.getStatus());
        }
        applyOutcome(run, outcome);
        if (pauseRequested && !outcome.complete()) {
            pauseRun(run);
        }
        return run;
    }

    private void applyOutcome(RentalHistoricalReconciliationRunDO run, BatchOutcome outcome) {
        run.setCursorAfterId(outcome.cursorAfterId());
        run.setScannedCount(value(run.getScannedCount()) + outcome.scanned());
        run.setSkippedCount(value(run.getSkippedCount()) + outcome.skipped());
        run.setCreatedCount(value(run.getCreatedCount()) + outcome.created());
        run.setUpdatedCount(value(run.getUpdatedCount()) + outcome.updated());
        run.setUnchangedCount(value(run.getUnchangedCount()) + outcome.unchanged());
        run.setConflictCount(value(run.getConflictCount()) + outcome.conflict());
        run.setReviewRequiredCount(value(run.getReviewRequiredCount()) + outcome.reviewRequired());
        if (outcome.complete()) {
            run.setStatus("SUCCEEDED");
            run.setFinishedAt(LocalDateTime.now(clock));
            clearExecution(run);
        } else {
            renewExecution(run);
        }
        run.setUpdater(SYSTEM_OPERATOR);
        runMapper.updateById(run);
    }

    private RentalHistoricalBackfillRunResult recordFailure(
            Long runId, String executionToken, Long channelOrderId,
            Long cursorBeforeId, Throwable exception) {
        return toResult(required(transactions.execute(status -> {
            RentalHistoricalReconciliationRunDO run = requireRunForUpdate(runId);
            if (!isCurrentExecution(run, executionToken)) {
                return run;
            }
            String errorCode = safeErrorCode(exception);
            if (channelOrderId != null) {
                RentalHistoricalReconciliationFailureDO failure =
                        RentalHistoricalReconciliationFailureDO.builder()
                                .runId(runId)
                                .channelOrderId(channelOrderId)
                                .cursorBeforeId(cursorBeforeId == null
                                        ? run.getCursorAfterId() : cursorBeforeId)
                                .attemptNo(value(run.getResumeCount()) + 1)
                                .errorCode(errorCode)
                                .build();
                failure.setCreator(SYSTEM_OPERATOR);
                failure.setUpdater(SYSTEM_OPERATOR);
                failureMapper.insert(failure);
            }

            run.setStatus("FAILED");
            run.setFailedCount(value(run.getFailedCount()) + 1);
            run.setLastFailedOrderId(channelOrderId);
            run.setLastErrorCode(errorCode);
            run.setFinishedAt(LocalDateTime.now(clock));
            clearExecution(run);
            run.setUpdater(SYSTEM_OPERATOR);
            runMapper.updateById(run);
            return run;
        })));
    }

    private RentalHistoricalReconciliationRunDO pauseAtBatchLimit(
            Long runId, String executionToken) {
        RentalHistoricalReconciliationRunDO run =
                requireOwnedRunForUpdate(runId, executionToken);
        if ("RUNNING".equals(run.getStatus())
                || "PAUSE_REQUESTED".equals(run.getStatus())) {
            pauseRun(run);
        }
        return run;
    }

    private RentalHistoricalReconciliationRunDO createRun(
            RentalHistoricalBackfillCommand command) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long currentMaxId = orderMapper.selectHistoricalReconciliationMaxId(tenantId);
        long effectiveEndId = Math.max(
                command.startAfterId(),
                Math.min(command.endIdInclusive(), currentMaxId == null ? 0L : currentMaxId));
        RentalHistoricalReconciliationRunDO run =
                RentalHistoricalReconciliationRunDO.builder()
                        .dryRun(command.dryRun())
                        .status("READY")
                        .startAfterId(command.startAfterId())
                        .endIdInclusive(effectiveEndId)
                        .cursorAfterId(command.startAfterId())
                        .batchSize(command.batchSize())
                        .resumeCount(0)
                        .scannedCount(0)
                        .skippedCount(0)
                        .createdCount(0)
                        .updatedCount(0)
                        .unchangedCount(0)
                        .conflictCount(0)
                        .failedCount(0)
                        .reviewRequiredCount(0)
                        .build();
        run.setTenantId(tenantId);
        run.setCreator(SYSTEM_OPERATOR);
        run.setUpdater(SYSTEM_OPERATOR);
        runMapper.insert(run);
        return run;
    }

    private RentalHistoricalReconciliationRunDO requireRunForUpdate(Long runId) {
        if (runId == null) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_NOT_EXISTS);
        }
        RentalHistoricalReconciliationRunDO run = runMapper.selectByIdForUpdate(runId);
        if (run == null) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_NOT_EXISTS);
        }
        return run;
    }

    private RentalHistoricalReconciliationRunDO requireOwnedRunForUpdate(
            Long runId, String executionToken) {
        RentalHistoricalReconciliationRunDO run = requireRunForUpdate(runId);
        if (!isCurrentExecution(run, executionToken)) {
            throw new ExecutionLeaseLostException();
        }
        return run;
    }

    private void requireRunnable(
            RentalHistoricalReconciliationRunDO run, LocalDateTime now) {
        boolean staleExecution = List.of("RUNNING", "PAUSE_REQUESTED").contains(run.getStatus())
                && !isActiveExecution(run, now);
        if (!List.of("READY", "PAUSED", "FAILED").contains(run.getStatus())
                && !staleExecution) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_STATE_INVALID, run.getStatus());
        }
    }

    private void pauseRun(RentalHistoricalReconciliationRunDO run) {
        run.setStatus("PAUSED");
        run.setPausedAt(LocalDateTime.now(clock));
        clearExecution(run);
        run.setUpdater(SYSTEM_OPERATOR);
        runMapper.updateById(run);
    }

    private void renewExecution(RentalHistoricalReconciliationRunDO run) {
        LocalDateTime now = LocalDateTime.now(clock);
        run.setHeartbeatAt(now);
        run.setLeaseUntil(now.plus(EXECUTION_LEASE));
    }

    private static void clearExecution(RentalHistoricalReconciliationRunDO run) {
        run.setExecutionToken(null);
        run.setLeaseUntil(null);
        run.setHeartbeatAt(null);
    }

    private static boolean isCurrentExecution(
            RentalHistoricalReconciliationRunDO run, String executionToken) {
        return run != null
                && List.of("RUNNING", "PAUSE_REQUESTED").contains(run.getStatus())
                && StringUtils.hasText(executionToken)
                && Objects.equals(run.getExecutionToken(), executionToken);
    }

    private static boolean isActiveExecution(
            RentalHistoricalReconciliationRunDO run, LocalDateTime now) {
        return isCurrentExecution(run, run.getExecutionToken())
                && run.getLeaseUntil() != null
                && run.getLeaseUntil().isAfter(now);
    }

    private void validateCommand(RentalHistoricalBackfillCommand command) {
        if (command == null) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_INVALID, "MISSING_COMMAND");
        }
        if (command.startAfterId() < 0 || command.endIdInclusive() <= command.startAfterId()) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_INVALID, "INVALID_ID_RANGE");
        }
        if (command.batchSize() < 1 || command.batchSize() > MAX_BATCH_SIZE) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_INVALID, "INVALID_BATCH_SIZE");
        }
        validateMaxBatches(command.maxBatches());
    }

    private void validateMaxBatches(int maxBatches) {
        if (maxBatches < 1 || maxBatches > MAX_BATCHES_PER_CALL) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_INVALID, "INVALID_MAX_BATCHES");
        }
    }

    private void requireWriteAuthorization(boolean dryRun, String confirmation) {
        if (dryRun) {
            return;
        }
        if (!writeEnabled) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_WRITE_DISABLED);
        }
        if (!WRITE_CONFIRMATION.equals(confirmation)) {
            throw exception(RENTAL_HISTORICAL_BACKFILL_INVALID, "CONFIRMATION_REQUIRED");
        }
    }

    private static OutcomeKind classify(
            RentalChannelOrderReconciliationResult result) {
        if (result == null) {
            return OutcomeKind.UNCHANGED;
        }
        return switch (result.mutationKind()) {
            case "SKIPPED" -> OutcomeKind.SKIPPED;
            case "CREATED" -> OutcomeKind.CREATED;
            case "UPDATED" -> OutcomeKind.UPDATED;
            case "CONFLICT_REVIEW" -> OutcomeKind.CONFLICT_REVIEW;
            default -> OutcomeKind.UNCHANGED;
        };
    }

    private static String safeErrorCode(Throwable throwable) {
        Throwable safe = throwable;
        for (int depth = 0; safe != null && depth < 8; depth++, safe = safe.getCause()) {
            if (safe instanceof ServiceException serviceException
                    && serviceException.getCode() != null) {
                return "SERVICE_" + serviceException.getCode();
            }
        }
        safe = throwable;
        while (safe != null && safe.getCause() != null) {
            safe = safe.getCause();
        }
        String simpleName = safe == null ? null : safe.getClass().getSimpleName();
        return StringUtils.hasText(simpleName) ? simpleName : "BACKFILL_FAILED";
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static <T> T required(T value) {
        return Objects.requireNonNull(value, "transaction result");
    }

    private static RentalHistoricalBackfillRunResult toResult(
            RentalHistoricalReconciliationRunDO run) {
        return new RentalHistoricalBackfillRunResult(
                run.getId(),
                Boolean.TRUE.equals(run.getDryRun()),
                run.getStatus(),
                run.getStartAfterId(),
                run.getEndIdInclusive(),
                run.getCursorAfterId(),
                run.getBatchSize(),
                value(run.getResumeCount()),
                value(run.getScannedCount()),
                value(run.getSkippedCount()),
                value(run.getCreatedCount()),
                value(run.getUpdatedCount()),
                value(run.getUnchangedCount()),
                value(run.getConflictCount()),
                value(run.getFailedCount()),
                value(run.getReviewRequiredCount()),
                run.getLastFailedOrderId(),
                run.getLastErrorCode());
    }

    private enum OutcomeKind {
        SKIPPED,
        CREATED,
        UPDATED,
        UNCHANGED,
        CONFLICT_REVIEW
    }

    private static final class MutableCounts {

        private int scanned;
        private int skipped;
        private int created;
        private int updated;
        private int unchanged;
        private int conflict;
        private int reviewRequired;

        void add(OutcomeKind kind) {
            scanned++;
            switch (kind) {
                case SKIPPED -> skipped++;
                case CREATED -> created++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
                case CONFLICT_REVIEW -> {
                    conflict++;
                    reviewRequired++;
                }
            }
        }

        BatchOutcome toOutcome(
                long cursorBeforeId, long cursorAfterId, boolean complete) {
            return new BatchOutcome(
                    cursorBeforeId, cursorAfterId, scanned, skipped, created, updated,
                    unchanged, conflict, reviewRequired, complete, false);
        }
    }

    private record BatchOutcome(long cursorBeforeId,
                                long cursorAfterId,
                                int scanned,
                                int skipped,
                                int created,
                                int updated,
                                int unchanged,
                                int conflict,
                                int reviewRequired,
                                boolean complete,
                                boolean paused) {

        static BatchOutcome emptyComplete(long cursorBeforeId, long cursorAfterId) {
            return new BatchOutcome(
                    cursorBeforeId, cursorAfterId, 0, 0, 0, 0, 0, 0, 0, true, false);
        }

        static BatchOutcome pausedOutcome() {
            return new BatchOutcome(0, 0, 0, 0, 0, 0, 0, 0, 0, false, true);
        }
    }

    private static final class BatchExecutionException extends RuntimeException {

        private final Long channelOrderId;
        private final long cursorBeforeId;

        private BatchExecutionException(
                Long channelOrderId, long cursorBeforeId, RuntimeException cause) {
            super(cause);
            this.channelOrderId = channelOrderId;
            this.cursorBeforeId = cursorBeforeId;
        }

        Long channelOrderId() {
            return channelOrderId;
        }

        long cursorBeforeId() {
            return cursorBeforeId;
        }
    }

    private static final class ExecutionLeaseLostException extends RuntimeException {
    }

}
