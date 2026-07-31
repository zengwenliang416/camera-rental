package cn.iocoder.yudao.module.rental.integration.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;

import java.time.LocalDateTime;

public record LogisticsTrackingEvent(
        LocalDateTime businessTime,
        String rawTime,
        RentalTrackingStatusEnum trackingStatus,
        String providerStatus,
        String traceText,
        String location,
        String source,
        Long inboxId
) {
}
