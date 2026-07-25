package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.time.LocalDateTime;

public record XianyuProductSnapshot(
        String externalProductId,
        String title,
        String categoryId,
        String status,
        LocalDateTime sourceUpdatedAt
) {
}
