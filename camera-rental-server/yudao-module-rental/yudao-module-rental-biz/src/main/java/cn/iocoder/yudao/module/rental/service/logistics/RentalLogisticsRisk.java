package cn.iocoder.yudao.module.rental.service.logistics;

import java.util.List;

public record RentalLogisticsRisk(
        String code,
        String severity,
        String safeMessage,
        String nextAction,
        List<Long> affectedDeviceIds
) {
}
