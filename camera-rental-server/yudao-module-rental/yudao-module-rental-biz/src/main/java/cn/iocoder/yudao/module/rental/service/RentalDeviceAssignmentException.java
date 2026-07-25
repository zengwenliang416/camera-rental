package cn.iocoder.yudao.module.rental.service;

/**
 * Typed domain failure for a future API boundary to translate without guessing.
 */
public class RentalDeviceAssignmentException extends RuntimeException {

    public enum Code {
        INVALID_COMMAND,
        IDEMPOTENCY_KEY_REUSED,
        ORDER_ITEM_NOT_FOUND,
        ORDER_NOT_ELIGIBLE,
        ITEM_ALREADY_FULLY_ASSIGNED,
        DEVICE_NOT_FOUND,
        DEVICE_NOT_ASSIGNABLE,
        DEVICE_MODEL_MISMATCH,
        SCHEDULE_CONFLICT
    }

    private final Code code;

    public RentalDeviceAssignmentException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

}
