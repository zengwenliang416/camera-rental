package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsCallbackCommand;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RentalDeliveryInboxWorker {

    private final RentalDeliveryInboxLeaseService leaseService;
    private final RentalDeliveryInboxCompletionService completionService;
    private final RentalLogisticsProviderRegistry providerRegistry;

    public RentalDeliveryInboxWorker(RentalDeliveryInboxLeaseService leaseService,
                                     RentalDeliveryInboxCompletionService completionService,
                                     RentalLogisticsProviderRegistry providerRegistry) {
        this.leaseService = leaseService;
        this.completionService = completionService;
        this.providerRegistry = providerRegistry;
    }

    public int processBatch(int limit) {
        List<RentalInboxWorkItem> inboxes = leaseService.claim(limit);
        for (RentalInboxWorkItem inbox : inboxes) {
            LogisticsOperationResult result;
            try {
                LogisticsProvider provider = providerRegistry.require(inbox.providerCode());
                result = provider.parseVerifiedCallback(new LogisticsCallbackCommand(
                        inbox.deliveryId(), inbox.inboxId(), inbox.callbackParams()));
            } catch (RuntimeException exception) {
                result = LogisticsOperationResult.failure("CALLBACK_PROCESSING_ERROR", true);
            }
            completionService.complete(inbox, result);
        }
        return inboxes.size();
    }
}
