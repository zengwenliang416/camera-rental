package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.time.LocalDateTime;

/**
 * Stable cursor facts extracted from one documented order-list item.
 * sellerRemark 用于检测“仅备注变更”：闲管家修改卖家备注不会刷新 update_time 也不触发推送，
 * 列表返回里带 seller_remark，同步时比对它来决定是否重拉详情。
 */
public record XianyuOrderListEntry(String externalOrderId, LocalDateTime sourceUpdatedAt,
                                   String sellerRemark) {
}
