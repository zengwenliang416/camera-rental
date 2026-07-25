package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.util.List;

public record XianyuAfterSalePage(List<XianyuAfterSaleSnapshot> entries, boolean hasNextPage) {
}
