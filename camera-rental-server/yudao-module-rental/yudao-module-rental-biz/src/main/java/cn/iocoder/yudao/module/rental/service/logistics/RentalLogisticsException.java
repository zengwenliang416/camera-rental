package cn.iocoder.yudao.module.rental.service.logistics;

public class RentalLogisticsException extends RuntimeException {

    private final String code;

    public RentalLogisticsException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
