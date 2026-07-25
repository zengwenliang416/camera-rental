package cn.iocoder.yudao.module.rental.integration.xianyu.service;

public record XianyuProductSkuSnapshot(
        String externalSkuId,
        String skuName,
        Integer stock
) {
}
