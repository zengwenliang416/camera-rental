package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Kuaidi100StatusMapper {

    public RentalTrackingStatusEnum map(String value) {
        if (value == null) {
            return RentalTrackingStatusEnum.UNKNOWN;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "0", "在途", "运输中", "TRANSIT" -> RentalTrackingStatusEnum.IN_TRANSIT;
            case "1", "揽收", "已揽收", "PICKUP" -> RentalTrackingStatusEnum.PICKED_UP;
            case "2", "13", "疑难", "清关异常", "EXCEPTION" -> RentalTrackingStatusEnum.EXCEPTION;
            case "3", "签收", "已签收", "DELIVERED" -> RentalTrackingStatusEnum.DELIVERED;
            case "4", "6", "14", "退签", "退回", "拒签", "RETURN" ->
                    RentalTrackingStatusEnum.RETURNING;
            case "5", "派件", "派送中", "DELIVERING" -> RentalTrackingStatusEnum.OUT_FOR_DELIVERY;
            case "8", "10", "11", "12", "清关", "待清关", "清关中", "已清关", "CUSTOMS" ->
                    RentalTrackingStatusEnum.CUSTOMS;
            default -> RentalTrackingStatusEnum.UNKNOWN;
        };
    }
}
