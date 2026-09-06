package cn.iocoder.yudao.module.rental.enums.rental;

/**
 * Delivery method of a manually created offline rental order.
 * EXPRESS orders are shipped through the offline express process; ERRAND and
 * SELF_DELIVERY orders are handed over without a waybill via confirm-outbound.
 */
public enum RentalDeliveryMethodEnum {

    EXPRESS,
    ERRAND,
    SELF_DELIVERY;

    public static RentalDeliveryMethodEnum of(String value) {
        if (value == null) {
            return null;
        }
        for (RentalDeliveryMethodEnum method : values()) {
            if (method.name().equals(value.trim())) {
                return method;
            }
        }
        return null;
    }

    public boolean requiresReceiverInfo() {
        return this == ERRAND || this == SELF_DELIVERY;
    }

}
