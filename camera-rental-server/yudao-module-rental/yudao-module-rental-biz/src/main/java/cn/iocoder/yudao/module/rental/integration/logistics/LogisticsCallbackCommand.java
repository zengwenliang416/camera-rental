package cn.iocoder.yudao.module.rental.integration.logistics;

public record LogisticsCallbackCommand(
        Long deliveryId,
        Long inboxId,
        String verifiedPayload
) {
}
