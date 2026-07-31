package cn.iocoder.yudao.module.rental.service.logistics;

import java.time.LocalDateTime;
import java.util.List;

public record NormalizedTrackingSnapshot(
        List<NormalizedTrackingEvent> events,
        String snapshotHash,
        LocalDateTime estimatedDeliveryAt,
        LocalDateTime synchronizedAt
) {
}
