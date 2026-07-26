package cn.iocoder.yudao.module.rental.integration.xianyu.client;

/**
 * Closed allowlist for XianGuanJia write endpoints enabled by this change.
 */
public enum XianyuWriteEndpoint {

    ORDER_SHIP("/api/open/order/ship");

    private final String path;

    XianyuWriteEndpoint(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
