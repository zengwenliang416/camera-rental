package cn.iocoder.yudao.module.rental.enums.returnregistration;

public enum ReturnAttachmentCategoryEnum {
    DEVICE_EXTERIOR(true, 6),
    SERIAL_LABEL(true, 6),
    PACKAGING(false, 6),
    DAMAGE_DETAIL(false, 6),
    RETURN_PHOTO(false, 10);

    private final boolean required;
    private final int maxCount;

    ReturnAttachmentCategoryEnum(boolean required, int maxCount) {
        this.required = required;
        this.maxCount = maxCount;
    }

    public boolean isRequired() {
        return required;
    }

    public int getMaxCount() {
        return maxCount;
    }
}
