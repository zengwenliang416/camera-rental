package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

public record XianyuProductPushPayload(
        String sellerId,
        String externalProductId,
        int productStatus,
        int publishStatus,
        int itemBizType,
        long price,
        int stock,
        long modifyTime
) {
}
