package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsProvider;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsQueryCommand;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsSubscribeCommand;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RentalDeliveryOutboxWorker {

    private final RentalDeliveryOutboxLeaseService leaseService;
    private final RentalDeliveryOutboxCompletionService completionService;
    private final RentalLogisticsProviderRegistry providerRegistry;

    public RentalDeliveryOutboxWorker(RentalDeliveryOutboxLeaseService leaseService,
                                      RentalDeliveryOutboxCompletionService completionService,
                                      RentalLogisticsProviderRegistry providerRegistry) {
        this.leaseService = leaseService;
        this.completionService = completionService;
        this.providerRegistry = providerRegistry;
    }

    public int processBatch(int limit) {
        List<RentalOutboxWorkItem> tasks = leaseService.claim(limit);
        for (RentalOutboxWorkItem task : tasks) {
            LogisticsOperationResult result;
            try {
                result = executeOutsideTransaction(task);
            } catch (RuntimeException exception) {
                result = LogisticsOperationResult.failure("PROVIDER_EXECUTION_ERROR", true);
            }
            completionService.complete(task, result);
        }
        return tasks.size();
    }

    private LogisticsOperationResult executeOutsideTransaction(RentalOutboxWorkItem task) {
        if (task.skipCode() != null) {
            return LogisticsOperationResult.skipped(task.skipCode());
        }
        LogisticsProvider provider = providerRegistry.require(task.providerCode());
        return switch (task.eventType()) {
            case SUBSCRIBE -> provider.subscribe(new LogisticsSubscribeCommand(task.deliveryId(),
                    task.credentialId(),
                    task.carrierCode(), task.waybillNo(), task.trackingPhone(),
                    task.callbackUrl(), task.callbackSalt()));
            case INITIAL_QUERY, REFRESH_QUERY, RECONCILE -> provider.query(new LogisticsQueryCommand(
                    task.deliveryId(), task.credentialId(),
                    task.carrierCode(), task.waybillNo(), task.trackingPhone()));
        };
    }
}
