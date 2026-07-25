package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import java.time.LocalDateTime;

public record XianyuAfterSaleSnapshot(
        String externalAfterSaleId,
        String externalOrderId,
        String afterSaleStatus,
        Long refundAmount,
        LocalDateTime timeoutAt,
        LocalDateTime sourceUpdatedAt,
        String payloadJson
) {
}
