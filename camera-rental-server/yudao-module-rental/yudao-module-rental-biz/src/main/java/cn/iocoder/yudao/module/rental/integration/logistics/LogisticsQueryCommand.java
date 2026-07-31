package cn.iocoder.yudao.module.rental.integration.logistics;

public record LogisticsQueryCommand(
        Long deliveryId,
        Long credentialId,
        String carrierCode,
        String waybillNo,
        String trackingPhone
) {
}
