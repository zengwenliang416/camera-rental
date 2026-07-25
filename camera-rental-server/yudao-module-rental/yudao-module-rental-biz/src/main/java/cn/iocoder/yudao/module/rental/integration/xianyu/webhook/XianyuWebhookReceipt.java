package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

public record XianyuWebhookReceipt(String result, String msg) {

    public static XianyuWebhookReceipt success() {
        return new XianyuWebhookReceipt("success", "接收成功");
    }

    public static XianyuWebhookReceipt fail(String message) {
        return new XianyuWebhookReceipt("fail", message);
    }

}
