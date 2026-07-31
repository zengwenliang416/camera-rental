package cn.iocoder.yudao.module.rental.service.logistics;

public record RentalInboxWorkItem(
        Long tenantId,
        Long inboxId,
        String processingToken,
        Long deliveryId,
        String providerCode,
        String callbackParams
) {
}
