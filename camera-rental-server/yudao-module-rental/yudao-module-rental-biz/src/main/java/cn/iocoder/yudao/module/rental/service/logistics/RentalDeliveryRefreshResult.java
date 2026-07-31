package cn.iocoder.yudao.module.rental.service.logistics;

import java.time.LocalDateTime;

public record RentalDeliveryRefreshResult(
        boolean accepted,
        String reason,
        LocalDateTime nextAllowedAt
) {
}
