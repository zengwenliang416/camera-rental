package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.util.List;

public record XianyuProductListPage(
        List<XianyuProductListEntry> entries,
        int count,
        int pageNo,
        int pageSize
) {
}
