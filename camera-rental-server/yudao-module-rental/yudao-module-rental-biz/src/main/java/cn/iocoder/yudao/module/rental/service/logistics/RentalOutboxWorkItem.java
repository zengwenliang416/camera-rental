package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;

public record RentalOutboxWorkItem(
        Long tenantId,
        Long outboxId,
        String processingToken,
        Long deliveryId,
        Long credentialId,
        RentalDeliveryOutboxEventTypeEnum eventType,
        String providerCode,
        String carrierCode,
        String waybillNo,
        String trackingPhone,
        String callbackUrl,
        String callbackSalt,
        String skipCode
) {
}
