package cn.iocoder.yudao.module.rental.enums.returnregistration;

public enum ReturnAttachmentCategoryEnum {
    DEVICE_EXTERIOR(true),
    SERIAL_LABEL(true),
    PACKAGING(false),
    DAMAGE_DETAIL(false);

    private final boolean required;

    ReturnAttachmentCategoryEnum(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }
}
