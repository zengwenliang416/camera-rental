package cn.iocoder.yudao.module.rental.service;

/**
 * Stable local conversion result for a future authorized job or admin endpoint.
 */
public record RentalConversionResult(String status, Long rentalOrderId, Long reviewId, String reasonCode) {

    public static RentalConversionResult converted(Long rentalOrderId) {
        return new RentalConversionResult("CONVERTED", rentalOrderId, null, null);
    }

    public static RentalConversionResult pending(String reasonCode) {
        return new RentalConversionResult("PENDING", null, null, reasonCode);
    }

    public static RentalConversionResult reviewRequired(Long reviewId, String reasonCode) {
        return new RentalConversionResult("REVIEW_REQUIRED", null, reviewId, reasonCode);
    }

}
