package cn.iocoder.yudao.module.rental.service.logistics;

public interface RentalDeliveryInboxService {

    Long accept(String providerCode, Long deliveryId, String providerTaskId, String payloadHash,
                String callbackParams);
}
