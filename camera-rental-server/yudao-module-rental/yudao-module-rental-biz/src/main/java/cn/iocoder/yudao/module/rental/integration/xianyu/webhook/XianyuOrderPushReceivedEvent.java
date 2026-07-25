package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

public record XianyuOrderPushReceivedEvent(
        Long tenantId,
        Long eventId,
        Long shopId,
        String externalOrderId) {
}
