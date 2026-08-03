package cn.iocoder.yudao.module.rental.service;

import java.math.BigDecimal;

public record SellerRemarkResolution(SellerRemarkRentalPeriod period, String source,
                                     BigDecimal confidence, String model,
                                     String evidenceJson) {

    public static SellerRemarkResolution rule(SellerRemarkRentalPeriod period) {
        return new SellerRemarkResolution(period, "RULE", null, null, null);
    }

}
