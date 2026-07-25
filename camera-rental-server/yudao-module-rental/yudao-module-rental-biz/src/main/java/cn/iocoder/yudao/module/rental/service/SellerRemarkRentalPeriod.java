package cn.iocoder.yudao.module.rental.service;

import java.time.LocalDate;

/**
 * Versioned result of parsing a channel seller remark into inclusive billable dates.
 */
public record SellerRemarkRentalPeriod(String version, String status, LocalDate billableStartDate,
                                       LocalDate billableEndDate, String reasonCode) {

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public static SellerRemarkRentalPeriod failure(String version, String reasonCode) {
        return new SellerRemarkRentalPeriod(version, "FAILED", null, null, reasonCode);
    }

    public static SellerRemarkRentalPeriod success(String version, LocalDate billableStartDate,
                                                   LocalDate billableEndDate) {
        return new SellerRemarkRentalPeriod(version, "SUCCESS", billableStartDate, billableEndDate, null);
    }

}
