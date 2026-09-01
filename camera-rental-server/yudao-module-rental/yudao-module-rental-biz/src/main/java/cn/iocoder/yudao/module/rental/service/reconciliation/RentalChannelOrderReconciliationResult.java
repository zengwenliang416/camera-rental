package cn.iocoder.yudao.module.rental.service.reconciliation;

public record RentalChannelOrderReconciliationResult(String status,
                                                     Long rentalOrderId,
                                                     Long reviewId,
                                                     String reasonCode,
                                                     String preparationStatus,
                                                     boolean planApplied,
                                                     String mutationKind) {

    public RentalChannelOrderReconciliationResult(
            String status, Long rentalOrderId, Long reviewId, String reasonCode,
            String preparationStatus, boolean planApplied) {
        this(status, rentalOrderId, reviewId, reasonCode, preparationStatus,
                planApplied, defaultMutationKind(status, planApplied));
    }

    public static RentalChannelOrderReconciliationResult converted(
            Long rentalOrderId, RentalOrderPreparationDecision preparation, boolean planApplied) {
        return converted(rentalOrderId, preparation, planApplied,
                planApplied ? "UPDATED" : "UNCHANGED");
    }

    public static RentalChannelOrderReconciliationResult converted(
            Long rentalOrderId, RentalOrderPreparationDecision preparation,
            boolean planApplied, String mutationKind) {
        return new RentalChannelOrderReconciliationResult(
                "CONVERTED", rentalOrderId, null, preparation.reasonCode(),
                preparation.status(), planApplied, mutationKind);
    }

    public static RentalChannelOrderReconciliationResult skipped() {
        return new RentalChannelOrderReconciliationResult(
                "CONFIG_SKIPPED", null, null, null, "CONFIG_SKIPPED", false, "SKIPPED");
    }

    public static RentalChannelOrderReconciliationResult ineligible(String reasonCode) {
        return new RentalChannelOrderReconciliationResult(
                "INELIGIBLE", null, null, reasonCode, "INELIGIBLE", false, "SKIPPED");
    }

    public static RentalChannelOrderReconciliationResult reviewRequired(Long reviewId, String reasonCode) {
        return new RentalChannelOrderReconciliationResult(
                "REVIEW_REQUIRED", null, reviewId, reasonCode,
                "REVIEW_REQUIRED", false, "CONFLICT_REVIEW");
    }

    private static String defaultMutationKind(String status, boolean planApplied) {
        return switch (status) {
            case "CONFIG_SKIPPED", "INELIGIBLE" -> "SKIPPED";
            case "REVIEW_REQUIRED" -> "CONFLICT_REVIEW";
            case "CONVERTED" -> planApplied ? "UPDATED" : "UNCHANGED";
            default -> "UNCHANGED";
        };
    }

}
