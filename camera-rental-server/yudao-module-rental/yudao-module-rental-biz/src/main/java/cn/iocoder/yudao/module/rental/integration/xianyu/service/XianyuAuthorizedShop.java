package cn.iocoder.yudao.module.rental.integration.xianyu.service;

/**
 * Normalized authorized shop row from XianGuanJia list API.
 */
public record XianyuAuthorizedShop(String authorizeId, String externalShopId, String xianyuUserName, String shopName,
                                   boolean valid, Long validEndTimeEpochSeconds, String guaranteeStatus) {
}
