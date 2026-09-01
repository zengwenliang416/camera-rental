package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.time.LocalDateTime;
import java.util.List;

public record XianyuProductSnapshot(
        String xgjProductId,
        List<XianyuPublishedItem> publishedItems,
        String title,
        String categoryId,
        String status,
        LocalDateTime sourceUpdatedAt
) {
}
