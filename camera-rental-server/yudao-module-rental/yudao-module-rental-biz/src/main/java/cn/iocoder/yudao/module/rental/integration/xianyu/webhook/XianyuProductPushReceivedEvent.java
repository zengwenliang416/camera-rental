package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

public record XianyuProductPushReceivedEvent(
        Long tenantId,
        Long eventId,
        Long shopId,
        String externalProductId
) {
}
