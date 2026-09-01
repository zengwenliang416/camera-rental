package cn.iocoder.yudao.module.rental.service.reconciliation;

public record RentalChannelReconciliationCounters(
        int scanned,
        int skipped,
        int created,
        int updated,
        int unchanged,
        int conflict,
        int failed,
        int reviewRequired) {

    public static RentalChannelReconciliationCounters empty() {
        return new RentalChannelReconciliationCounters(0, 0, 0, 0, 0, 0, 0, 0);
    }

    public RentalChannelReconciliationCounters record(
            RentalChannelOrderReconciliationResult result) {
        String mutationKind = result == null ? "UNCHANGED" : result.mutationKind();
        int nextReview = result != null
                && (result.reviewId() != null || "REVIEW_REQUIRED".equals(result.status())
                || "REVIEW_REQUIRED".equals(result.preparationStatus())) ? 1 : 0;
        return new RentalChannelReconciliationCounters(
                scanned + 1,
                skipped + ("SKIPPED".equals(mutationKind) ? 1 : 0),
                created + ("CREATED".equals(mutationKind) ? 1 : 0),
                updated + ("UPDATED".equals(mutationKind) ? 1 : 0),
                unchanged + ("UNCHANGED".equals(mutationKind) ? 1 : 0),
                conflict + ("CONFLICT_REVIEW".equals(mutationKind) ? 1 : 0),
                failed,
                reviewRequired + nextReview);
    }

    public RentalChannelReconciliationCounters recordFailure() {
        return new RentalChannelReconciliationCounters(
                scanned + 1, skipped, created, updated, unchanged, conflict,
                failed + 1, reviewRequired);
    }

}
