package cn.iocoder.yudao.module.rental.enums.rental;

public enum RentalDeviceLockTypeEnum {
    ORDER_HOLD(true),
    RETURN_INSPECTION(false),
    MAINTENANCE(false),
    MANUAL_HOLD(true);

    private final boolean manuallyManaged;

    RentalDeviceLockTypeEnum(boolean manuallyManaged) {
        this.manuallyManaged = manuallyManaged;
    }

    public boolean isManuallyManaged() {
        return manuallyManaged;
    }
}
