package cn.iocoder.yudao.module.rental.integration.xianyu.client;

/**
 * The complete outbound allowlist for the V1 XianGuanJia read client.
 */
public enum XianyuReadEndpoint {

    AUTHORIZED_SHOPS("/api/open/user/authorize/list"),
    PRODUCT_CATEGORIES("/api/open/product/category/list"),
    PRODUCT_PROPERTIES("/api/open/product/pv/list"),
    PRODUCTS("/api/open/product/list"),
    PRODUCT_DETAIL("/api/open/product/detail"),
    PRODUCT_SKUS("/api/open/product/sku/list"),
    ORDERS("/api/open/order/list"),
    ORDER_DETAIL("/api/open/order/detail"),
    AFTER_SALES("/api/open/trade/refund/list"),
    AFTER_SALE_DETAIL("/api/open/trade/refund/detail"),
    EXPRESS_COMPANIES("/api/open/express/companies");

    private final String path;

    XianyuReadEndpoint(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
