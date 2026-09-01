package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelReconciliationRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelReconciliationRunMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_RECONCILIATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_RECONCILIATION_ACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalChannelReconciliationRunServiceTest {

    @Mock
    private RentalChannelReconciliationRunMapper runMapper;

    private RentalChannelReconciliationRunService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        service = new RentalChannelReconciliationRunService(runMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createPersistsTenantScopedPendingRun() {
        org.mockito.Mockito.doAnswer(invocation -> {
            RentalChannelReconciliationRunDO run = invocation.getArgument(0);
            run.setId(80L);
            return 1;
        }).when(runMapper).insert(
                org.mockito.ArgumentMatchers.any(RentalChannelReconciliationRunDO.class));

        assertEquals(80L, service.createRuleChangeRun(40L, 7L, "item-1"));

        ArgumentCaptor<RentalChannelReconciliationRunDO> captor =
                ArgumentCaptor.forClass(RentalChannelReconciliationRunDO.class);
        verify(runMapper).insert(captor.capture());
        RentalChannelReconciliationRunDO run = captor.getValue();
        assertEquals(9L, run.getTenantId());
        assertEquals("PENDING", run.getStatus());
        assertEquals(0, run.getScannedCount());
    }

    @Test
    void getUsesCurrentTenantAndRejectsForeignOrMissingRun() {
        when(runMapper.selectByTenantIdAndId(9L, 80L)).thenReturn(null);

        assertServiceException(() -> service.get(80L), RENTAL_CHANNEL_RECONCILIATION_NOT_EXISTS);
        verify(runMapper).selectByTenantIdAndId(9L, 80L);
    }

    @Test
    void activeRuleRunBlocksAnotherMutationWithinCurrentTenant() {
        when(runMapper.existsActiveByTenantIdAndProductRuleId(9L, 40L)).thenReturn(true);

        assertServiceException(() -> service.assertNoActiveRuleRun(40L),
                RENTAL_CHANNEL_RECONCILIATION_ACTIVE);

        verify(runMapper).existsActiveByTenantIdAndProductRuleId(9L, 40L);
    }

    @Test
    void transitionsPersistCountersAndTerminalStatuses() {
        RentalChannelReconciliationRunDO run = RentalChannelReconciliationRunDO.builder()
                .id(80L)
                .status("PENDING")
                .build();
        run.setTenantId(9L);
        when(runMapper.selectByTenantIdAndId(9L, 80L)).thenReturn(run);

        service.markRunning(80L);
        assertEquals("RUNNING", run.getStatus());
        assertNotNull(run.getStartedAt());
        assertNull(run.getFinishedAt());

        service.complete(80L, new RentalChannelReconciliationCounters(
                8, 1, 2, 3, 1, 0, 1, 1));
        assertEquals("COMPLETED_WITH_ERRORS", run.getStatus());
        assertEquals(8, run.getScannedCount());
        assertEquals(1, run.getFailedCount());
        assertNotNull(run.getFinishedAt());

        service.fail(80L, new RentalChannelReconciliationCounters(
                9, 1, 2, 3, 1, 1, 2, 2), "IllegalStateException");
        assertEquals("FAILED", run.getStatus());
        assertEquals("IllegalStateException", run.getLastErrorCode());
        assertEquals(2, run.getReviewRequiredCount());
        verify(runMapper, org.mockito.Mockito.times(3)).updateById(run);
    }

}
