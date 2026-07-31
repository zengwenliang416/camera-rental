package cn.iocoder.yudao.module.rental.enums.logistics;

public enum RentalTrackingStatusEnum {
    CREATED,
    INFO_RECEIVED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION,
    RETURNING,
    RETURNED,
    CUSTOMS,
    UNKNOWN;

    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED;
    }
}
