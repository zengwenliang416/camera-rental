package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

public record Kuaidi100CallbackReceipt(
        boolean result,
        String returnCode,
        String message
) {

    public static Kuaidi100CallbackReceipt success() {
        return new Kuaidi100CallbackReceipt(true, "200", "成功");
    }

    public static Kuaidi100CallbackReceipt failure() {
        return new Kuaidi100CallbackReceipt(false, "500", "失败");
    }
}
