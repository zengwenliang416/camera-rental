package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryInboxWorkerTest {

    private final RentalDeliveryInboxLeaseService leaseService = mock(RentalDeliveryInboxLeaseService.class);
    private final RentalDeliveryInboxCompletionService completionService =
            mock(RentalDeliveryInboxCompletionService.class);
    private final RentalLogisticsProviderRegistry registry = mock(RentalLogisticsProviderRegistry.class);
    private final LogisticsProvider provider = mock(LogisticsProvider.class);
    private final RentalDeliveryInboxWorker worker =
            new RentalDeliveryInboxWorker(leaseService, completionService, registry);

    @Test
    void parsesVerifiedPayloadAndCompletesLeasedInbox() {
        RentalInboxWorkItem work = new RentalInboxWorkItem(
                9L, 10L, "lease-token", 20L, "KUAIDI100", "{\"lastResult\":{}}");
        LogisticsOperationResult result = LogisticsOperationResult.success(null, null);
        when(leaseService.claim(20)).thenReturn(List.of(work));
        when(registry.require("KUAIDI100")).thenReturn(provider);
        when(provider.parseVerifiedCallback(any())).thenReturn(result);

        int processed = worker.processBatch(20);

        assertEquals(1, processed);
        verify(provider).parseVerifiedCallback(any());
        verify(completionService).complete(work, result);
    }

    @Test
    void convertsUnexpectedCallbackFailureIntoRetryableSafeResult() {
        RentalInboxWorkItem work = new RentalInboxWorkItem(
                9L, 10L, "lease-token", 20L, "KUAIDI100", "{\"lastResult\":{}}");
        when(leaseService.claim(20)).thenReturn(List.of(work));
        when(registry.require("KUAIDI100")).thenReturn(provider);
        when(provider.parseVerifiedCallback(any())).thenThrow(new IllegalArgumentException("raw callback"));

        worker.processBatch(20);

        ArgumentCaptor<LogisticsOperationResult> result =
                ArgumentCaptor.forClass(LogisticsOperationResult.class);
        verify(completionService).complete(org.mockito.ArgumentMatchers.eq(work), result.capture());
        assertEquals("CALLBACK_PROCESSING_ERROR", result.getValue().safeCode());
        assertEquals(true, result.getValue().retryable());
    }
}
