package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.util.List;

/**
 * Validated page metadata from the documented order-list response.
 */
public record XianyuOrderListPage(List<XianyuOrderListEntry> entries, int count, int pageNo, int pageSize) {
}
