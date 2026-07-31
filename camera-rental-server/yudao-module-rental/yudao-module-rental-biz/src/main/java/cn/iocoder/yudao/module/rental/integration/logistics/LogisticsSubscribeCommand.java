package cn.iocoder.yudao.module.rental.integration.logistics;

public record LogisticsSubscribeCommand(
        Long deliveryId,
        Long credentialId,
        String carrierCode,
        String waybillNo,
        String trackingPhone,
        String callbackUrl,
        String callbackSalt
) {
}
