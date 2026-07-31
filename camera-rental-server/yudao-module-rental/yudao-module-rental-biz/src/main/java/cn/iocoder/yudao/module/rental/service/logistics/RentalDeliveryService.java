package cn.iocoder.yudao.module.rental.service.logistics;

public interface RentalDeliveryService {

    RentalDeliveryResult createOrReuse(RentalDeliveryCreateCommand command);

    RentalDeliveryResult createOrReuseLocalOnly(RentalDeliveryCreateCommand command);

    RentalDeliveryResult getResult(Long deliveryId);
}
