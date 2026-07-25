package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

public record XianyuOrderPushPayload(
        String sellerId,
        String externalOrderId,
        int orderType,
        int orderStatus,
        int refundStatus,
        long modifyTime,
        long productId,
        long itemId) {
}
