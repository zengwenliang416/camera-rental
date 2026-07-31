package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;

import java.time.LocalDateTime;

public record NormalizedTrackingEvent(
        int sequence,
        String fingerprint,
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
