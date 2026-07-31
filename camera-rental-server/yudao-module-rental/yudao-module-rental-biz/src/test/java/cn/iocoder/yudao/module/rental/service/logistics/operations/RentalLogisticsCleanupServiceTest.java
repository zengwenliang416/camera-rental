package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.CleanupCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RentalLogisticsCleanupServiceTest {

    private final RentalLogisticsOperationsMapper operationsMapper = mock(RentalLogisticsOperationsMapper.class);
    private final RentalLogisticsCleanupService service = new RentalLogisticsCleanupService(operationsMapper);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void dryRunCountsOnlyAndRespectsSingleGlobalLimit() {
        when(operationsMapper.countCleanupTraces(eq(9L), any(), eq(10))).thenReturn(4);
        when(operationsMapper.countCleanupInbox(eq(9L), any(), eq(6))).thenReturn(3);
        when(operationsMapper.countCleanupOutbox(eq(9L), any(), eq(3))).thenReturn(2);

        var result = service.cleanup(new CleanupCommand(true, 90, 10));

        assertTrue(result.dryRun());
        assertEquals(9, result.totalCount());
        verify(operationsMapper, never()).deleteCleanupTraces(anyLong(), any(), anyInt());
        verify(operationsMapper, never()).deleteCleanupInbox(anyLong(), any(), anyInt());
        verify(operationsMapper, never()).deleteCleanupOutbox(anyLong(), any(), anyInt());
    }

    @Test
    void executionDeletesOnlyThroughBoundedEligibleTechnicalQueries() {
        when(operationsMapper.deleteCleanupTraces(eq(9L), any(), eq(5))).thenReturn(2);
        when(operationsMapper.deleteCleanupInbox(eq(9L), any(), eq(3))).thenReturn(1);
        when(operationsMapper.deleteCleanupOutbox(eq(9L), any(), eq(2))).thenReturn(2);

        var result = service.cleanup(new CleanupCommand(false, 365, 5));

        assertFalse(result.dryRun());
        assertEquals(5, result.totalCount());
        verify(operationsMapper).deleteCleanupTraces(eq(9L), any(), eq(5));
        verify(operationsMapper).deleteCleanupInbox(eq(9L), any(), eq(3));
        verify(operationsMapper).deleteCleanupOutbox(eq(9L), any(), eq(2));
    }

    @Test
    void rejectsUnsafeRetentionAndUnboundedLimit() {
        RentalLogisticsException retention = assertThrows(RentalLogisticsException.class,
                () -> service.cleanup(new CleanupCommand(true, 29, 10)));
        assertEquals("CLEANUP_RETENTION_OUT_OF_RANGE", retention.getCode());
        RentalLogisticsException limit = assertThrows(RentalLogisticsException.class,
                () -> service.cleanup(new CleanupCommand(true, 90, 1001)));
        assertEquals("CLEANUP_LIMIT_OUT_OF_RANGE", limit.getCode());
        verifyNoInteractions(operationsMapper);
    }
}
