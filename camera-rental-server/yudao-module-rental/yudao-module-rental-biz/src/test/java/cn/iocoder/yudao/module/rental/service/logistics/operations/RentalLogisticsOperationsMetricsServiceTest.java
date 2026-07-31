package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

class RentalLogisticsOperationsMetricsServiceTest {

    private final RentalLogisticsOperationsMapper mapper = mock(RentalLogisticsOperationsMapper.class);
    private final RentalLogisticsMetricsService service = new RentalLogisticsMetricsService(mapper);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void metricsContainOnlySafeAggregates() {
        RentalLogisticsOperationsMapper.LogisticsMetricsRow row =
                new RentalLogisticsOperationsMapper.LogisticsMetricsRow();
        row.setDeliveryCount(12L);
        row.setStaleDeliveryCount(3L);
        row.setFailedOutboxCount(2L);
        row.setFailedInboxCount(1L);
        row.setRetriedOutboxCount(4L);
        row.setRetriedInboxCount(5L);
        row.setAverageOutboxDelaySeconds(8L);
        when(mapper.selectMetrics(eq(9L), any())).thenReturn(row);
        when(mapper.selectDeliveryStatusCounts(9L)).thenReturn(List.of(status("IN_TRANSIT", 7L)));
        when(mapper.selectOutboxStatusCounts(9L)).thenReturn(List.of(status("FAILED", 2L)));
        when(mapper.selectInboxStatusCounts(9L)).thenReturn(List.of(status("SUCCEEDED", 6L)));

        LocalDateTime before = LocalDateTime.now().minusHours(24).minusSeconds(2);
        var metrics = service.getMetrics();
        LocalDateTime after = LocalDateTime.now().minusHours(24).plusSeconds(2);

        assertEquals(12, metrics.deliveryCount());
        assertEquals(3, metrics.staleDeliveryCount());
        assertEquals(3, metrics.failedOutboxCount() + metrics.failedInboxCount());
        assertEquals(9, metrics.retriedOutboxCount() + metrics.retriedInboxCount());
        assertEquals(8, metrics.averageOutboxDelaySeconds());
        assertEquals(7, metrics.deliveryStatusCounts().get("IN_TRANSIT"));
        assertEquals(2, metrics.outboxStatusCounts().get("FAILED"));
        assertEquals(6, metrics.inboxStatusCounts().get("SUCCEEDED"));
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).selectMetrics(eq(9L), cutoff.capture());
        assertFalse(cutoff.getValue().isBefore(before));
        assertTrue(cutoff.getValue().isBefore(after));
    }

    private RentalLogisticsOperationsMapper.StatusCountRow status(String status, Long count) {
        RentalLogisticsOperationsMapper.StatusCountRow row =
                new RentalLogisticsOperationsMapper.StatusCountRow();
        row.setStatus(status);
        row.setCount(count);
        return row;
    }
}
