package cn.iocoder.yudao.module.rental.service;

import java.time.LocalDate;

/**
 * Versioned result of parsing a channel seller remark into inclusive billable dates.
 */
public record SellerRemarkRentalPeriod(String version, String status, LocalDate billableStartDate,
                                       LocalDate billableEndDate, LocalDate shipDate, LocalDate receiveDate,
                                       LocalDate returnDate, String reasonCode) {

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public static SellerRemarkRentalPeriod pending(String version, String reasonCode) {
        return new SellerRemarkRentalPeriod(version, "PENDING", null, null, null, null, null, reasonCode);
    }

    public static SellerRemarkRentalPeriod pending(String version, LocalDate billableStartDate,
                                                   LocalDate billableEndDate, LocalDate shipDate,
                                                   LocalDate receiveDate, LocalDate returnDate,
                                                   String reasonCode) {
        return new SellerRemarkRentalPeriod(version, "PENDING", billableStartDate, billableEndDate,
                shipDate, receiveDate, returnDate, reasonCode);
    }

    public static SellerRemarkRentalPeriod failure(String version, String reasonCode) {
        return new SellerRemarkRentalPeriod(version, "FAILED", null, null, null, null, null, reasonCode);
    }

    public static SellerRemarkRentalPeriod success(String version, LocalDate billableStartDate,
                                                   LocalDate billableEndDate) {
        return success(version, billableStartDate, billableEndDate, null, null, null);
    }

    public static SellerRemarkRentalPeriod success(String version, LocalDate billableStartDate,
                                                   LocalDate billableEndDate, LocalDate shipDate,
                                                   LocalDate receiveDate, LocalDate returnDate) {
        return new SellerRemarkRentalPeriod(version, "SUCCESS", billableStartDate, billableEndDate,
                shipDate, receiveDate, returnDate, null);
    }

    public LocalDate occupyStartDate() {
        return shipDate;
    }

    public LocalDate occupyEndDateExclusive() {
        return returnDate == null ? null : returnDate.plusDays(1);
    }

}
