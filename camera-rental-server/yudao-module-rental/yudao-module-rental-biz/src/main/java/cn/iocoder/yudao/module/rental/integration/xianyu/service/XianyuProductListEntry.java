package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.time.LocalDateTime;

public record XianyuProductListEntry(
        String externalProductId,
        LocalDateTime sourceUpdatedAt,
        int specType
) {
}
