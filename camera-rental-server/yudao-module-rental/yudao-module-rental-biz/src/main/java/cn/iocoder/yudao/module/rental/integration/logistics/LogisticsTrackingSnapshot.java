package cn.iocoder.yudao.module.rental.integration.logistics;

import java.time.LocalDateTime;
import java.util.List;

public record LogisticsTrackingSnapshot(
        List<LogisticsTrackingEvent> events,
        LocalDateTime estimatedDeliveryAt,
        LocalDateTime synchronizedAt
) {

    public LogisticsTrackingSnapshot {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
