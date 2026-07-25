package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.time.LocalDateTime;

/**
 * Parsed order-detail facts for normalized columns.
 * Full source {@code data} object is also kept as {@link #detailJson}.
 */
public record XianyuOrderSnapshot(
        String externalOrderId,
        String externalProductId,
        String externalSkuId,
        String orderStatus,
        Long payAmount,
        String sellerRemark,
        LocalDateTime sourceCreatedAt,
        LocalDateTime sourceUpdatedAt,
        /** Full order-detail {@code data} object JSON (all API fields). */
        String detailJson,
        Integer orderType,
        LocalDateTime orderTime,
        Long totalAmount,
        String payNo,
        LocalDateTime payTime,
        Integer refundStatus,
        Long refundAmount,
        LocalDateTime refundTime,
        String waybillNo,
        String expressCode,
        String expressName,
        Long expressFee,
        Integer consignType,
        LocalDateTime consignTime,
        LocalDateTime confirmTime,
        String cancelReason,
        LocalDateTime cancelTime,
        String buyerNick,
        String sellerName,
        String goodsTitle,
        Integer goodsQuantity,
        Long goodsPrice,
        String goodsJson,
        Long xybSellerAmount,
        Boolean taxIncluded,
        Integer idleBizType,
        Integer pinGroupStatus
) {
}
