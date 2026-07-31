package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.MetricsView;

@Service
public class RentalLogisticsMetricsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int STALE_HOURS = 24;

    private final RentalLogisticsOperationsMapper operationsMapper;

    public RentalLogisticsMetricsService(RentalLogisticsOperationsMapper operationsMapper) {
        this.operationsMapper = operationsMapper;
    }

    public MetricsView getMetrics() {
        RentalLogisticsOperationsMapper.LogisticsMetricsRow row = operationsMapper.selectMetrics(
                TenantContextHolder.getRequiredTenantId(),
                LocalDateTime.now(BUSINESS_ZONE).minusHours(STALE_HOURS));
        if (row == null) {
            return new MetricsView(0, Map.of(), Map.of(), Map.of(),
                    0, 0, 0, 0, 0, 0, null, null);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return new MetricsView(value(row.getDeliveryCount()),
                toCounts(operationsMapper.selectDeliveryStatusCounts(tenantId)),
                toCounts(operationsMapper.selectOutboxStatusCounts(tenantId)),
                toCounts(operationsMapper.selectInboxStatusCounts(tenantId)),
                value(row.getStaleDeliveryCount()),
                value(row.getFailedOutboxCount()), value(row.getFailedInboxCount()),
                value(row.getRetriedOutboxCount()), value(row.getRetriedInboxCount()),
                value(row.getAverageOutboxDelaySeconds()), row.getLastOutboxSuccessAt(),
                row.getLastInboxSuccessAt());
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private Map<String, Long> toCounts(List<RentalLogisticsOperationsMapper.StatusCountRow> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (rows == null) {
            return result;
        }
        for (RentalLogisticsOperationsMapper.StatusCountRow row : rows) {
            if (row != null && row.getStatus() != null) {
                result.put(row.getStatus(), value(row.getCount()));
            }
        }
        return result;
    }
}
