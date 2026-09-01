package cn.iocoder.yudao.module.rental.service.reconciliation;

import java.util.List;

public record RentalChannelOrderReconciliationRequestedEvent(
        Long tenantId,
        Scope scope,
        Long shopId,
        String xianyuItemId,
        String xgjProductId,
        List<String> xgjSkuIds,
        Long reconciliationRunId) {

    public RentalChannelOrderReconciliationRequestedEvent {
        xgjSkuIds = xgjSkuIds == null ? List.of() : List.copyOf(xgjSkuIds);
    }

    public static RentalChannelOrderReconciliationRequestedEvent forItem(
            Long tenantId, Long shopId, String xianyuItemId) {
        return new RentalChannelOrderReconciliationRequestedEvent(
                tenantId, Scope.ITEM, shopId, xianyuItemId, null, List.of(), null);
    }

    public static RentalChannelOrderReconciliationRequestedEvent forTrackedItem(
            Long tenantId, Long shopId, String xianyuItemId, Long reconciliationRunId) {
        return new RentalChannelOrderReconciliationRequestedEvent(
                tenantId, Scope.ITEM, shopId, xianyuItemId, null, List.of(),
                reconciliationRunId);
    }

    public static RentalChannelOrderReconciliationRequestedEvent forProduct(
            Long tenantId, Long shopId, String xgjProductId) {
        return new RentalChannelOrderReconciliationRequestedEvent(
                tenantId, Scope.PRODUCT, shopId, null, xgjProductId, List.of(), null);
    }

    public static RentalChannelOrderReconciliationRequestedEvent forProductSkus(
            Long tenantId, Long shopId, String xgjProductId, List<String> xgjSkuIds) {
        return new RentalChannelOrderReconciliationRequestedEvent(
                tenantId, Scope.PRODUCT_SKUS, shopId, null, xgjProductId, xgjSkuIds, null);
    }

    public enum Scope {
        ITEM,
        PRODUCT,
        PRODUCT_SKUS
    }

}
