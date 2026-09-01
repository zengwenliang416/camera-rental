package cn.iocoder.yudao.module.rental.integration.xianyu.service;

public record XianyuProductSkuSnapshot(
        String xgjSkuId,
        String xianyuSkuId,
        String skuName,
        Integer stock
) {
}
