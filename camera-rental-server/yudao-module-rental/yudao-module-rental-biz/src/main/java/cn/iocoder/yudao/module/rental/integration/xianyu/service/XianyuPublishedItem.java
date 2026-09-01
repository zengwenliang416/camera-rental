package cn.iocoder.yudao.module.rental.integration.xianyu.service;

/**
 * One Xianyu item published from a XianGuanJia product to an exact Xianyu account.
 */
public record XianyuPublishedItem(
        String xianyuUserName,
        String xianyuItemId,
        Integer status
) {
}
