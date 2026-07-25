package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.util.List;

public record XianyuProductSkuGroup(
        String externalProductId,
        List<XianyuProductSkuSnapshot> skuItems
) {
}
