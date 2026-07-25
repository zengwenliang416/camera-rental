package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.time.LocalDateTime;

/**
 * Stable cursor facts extracted from one documented order-list item.
 */
public record XianyuOrderListEntry(String externalOrderId, LocalDateTime sourceUpdatedAt) {
}
