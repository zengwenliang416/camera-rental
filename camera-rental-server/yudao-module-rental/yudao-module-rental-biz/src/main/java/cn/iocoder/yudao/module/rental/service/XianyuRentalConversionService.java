package cn.iocoder.yudao.module.rental.service;

public interface XianyuRentalConversionService {

    /**
     * Convert one durable channel order into an internal rental order, or open a manual review.
     */
    RentalConversionResult convert(Long channelOrderId);

    /**
     * Convert an order during shipment after the operator selected a concrete device.
     * The selected device model is treated as explicit mapping evidence when no mapping exists yet.
     */
    RentalConversionResult convertForShipment(Long channelOrderId, String equipmentModelCode);

    /**
     * Hermes-style: after a channel order is persisted, attempt conversion automatically.
     * Soft business outcomes become {@code REVIEW_REQUIRED}; unexpected errors are logged only
     * so order sync / push ingestion is never rolled back by conversion failures.
     */
    void autoConvertAfterPersist(Long channelOrderId);

}
