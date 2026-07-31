package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class RentalDeliveryOutboxWorkerTest {

    private final RentalDeliveryOutboxLeaseService leaseService = mock(RentalDeliveryOutboxLeaseService.class);
    private final RentalDeliveryOutboxCompletionService completionService =
            mock(RentalDeliveryOutboxCompletionService.class);
    private final RentalLogisticsProviderRegistry registry = mock(RentalLogisticsProviderRegistry.class);
    private final LogisticsProvider provider = mock(LogisticsProvider.class);
    private final RentalDeliveryOutboxWorker worker =
            new RentalDeliveryOutboxWorker(leaseService, completionService, registry);

    @Test
    void invokesProviderBetweenTransactionalLeaseAndCompletionBoundaries() throws Exception {
        RentalOutboxWorkItem work = new RentalOutboxWorkItem(9L, 10L, "lease-token", 20L,
                30L, RentalDeliveryOutboxEventTypeEnum.INITIAL_QUERY, "KUAIDI100", "shunfeng",
                "SF0000000001", null, null, null, null);
        LogisticsOperationResult result = LogisticsOperationResult.success(null, null);
        when(leaseService.claim(20)).thenReturn(List.of(work));
        when(registry.require("KUAIDI100")).thenReturn(provider);
        when(provider.query(any())).thenReturn(result);

        int processed = worker.processBatch(20);

        assertTrue(processed == 1);
        verify(provider).query(any());
        verify(completionService).complete(work, result);
        assertFalse(RentalDeliveryOutboxWorker.class.getMethod("processBatch", int.class)
                .isAnnotationPresent(Transactional.class));
        assertTrue(RentalDeliveryOutboxLeaseService.class.getMethod("claim", int.class)
                .isAnnotationPresent(Transactional.class));
        assertTrue(RentalDeliveryOutboxCompletionService.class
                .getMethod("complete", RentalOutboxWorkItem.class, LogisticsOperationResult.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void convertsUnexpectedProviderFailureIntoRetryableSafeResult() {
        RentalOutboxWorkItem work = new RentalOutboxWorkItem(9L, 10L, "lease-token", 20L,
                30L, RentalDeliveryOutboxEventTypeEnum.INITIAL_QUERY, "KUAIDI100", "shunfeng",
                "SF0000000001", null, null, null, null);
        when(leaseService.claim(20)).thenReturn(List.of(work));
        when(registry.require("KUAIDI100")).thenReturn(provider);
        when(provider.query(any())).thenThrow(new IllegalStateException("sensitive provider failure"));

        worker.processBatch(20);

        ArgumentCaptor<LogisticsOperationResult> result =
                ArgumentCaptor.forClass(LogisticsOperationResult.class);
        verify(completionService).complete(org.mockito.ArgumentMatchers.eq(work), result.capture());
        assertTrue(result.getValue().retryable());
        assertTrue("PROVIDER_EXECUTION_ERROR".equals(result.getValue().safeCode()));
    }
}
