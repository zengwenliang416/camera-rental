package cn.iocoder.yudao.module.rental.service.logistics;

import java.util.List;

public record RentalDeliveryResult(
        Long deliveryId,
        boolean created,
        String mappingStatus,
        String subscribeStatus,
        String queryStatus,
        String maskedWaybillNo,
        String reasonCode,
        List<String> pendingEventTypes
) {

    public RentalDeliveryResult {
        pendingEventTypes = pendingEventTypes == null ? List.of() : List.copyOf(pendingEventTypes);
    }
}
