package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
public class RentalDeliveryTrackingRefreshService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> QUERY_EVENT_TYPES = Set.of(
            RentalDeliveryOutboxEventTypeEnum.INITIAL_QUERY.name(),
            RentalDeliveryOutboxEventTypeEnum.REFRESH_QUERY.name(),
            RentalDeliveryOutboxEventTypeEnum.RECONCILE.name());

    private final RentalDeliveryMapper deliveryMapper;
    private final RentalLogisticsProviderConfigService configService;
    private final RentalDeliveryOutboxService outboxService;
    private final Clock clock;

    public RentalDeliveryTrackingRefreshService(RentalDeliveryMapper deliveryMapper,
                                                RentalLogisticsProviderConfigService configService,
                                                RentalDeliveryOutboxService outboxService) {
        this(deliveryMapper, configService, outboxService, Clock.system(BUSINESS_ZONE));
    }

    RentalDeliveryTrackingRefreshService(RentalDeliveryMapper deliveryMapper,
                                         RentalLogisticsProviderConfigService configService,
                                         RentalDeliveryOutboxService outboxService,
                                         Clock clock) {
        this.deliveryMapper = deliveryMapper;
        this.configService = configService;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeliveryRefreshResult refresh(Long deliveryId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndIdForUpdate(tenantId, deliveryId);
        if (delivery == null) {
            return rejected("DELIVERY_NOT_FOUND", null);
        }
        if (!"READY".equals(delivery.getMappingStatus())
                || !StringUtils.hasText(delivery.getProviderCode())
                || !StringUtils.hasText(delivery.getProviderCarrierCode())) {
            return rejected("MAPPING_REQUIRED", delivery.getNextQueryAllowedAt());
        }

        RentalLogisticsProviderConfigDO config = configService.get(delivery.getProviderCode());
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return rejected("PROVIDER_DISABLED", delivery.getNextQueryAllowedAt());
        }
        if (!Boolean.TRUE.equals(config.getQueryEnabled())) {
            return rejected("QUERY_DISABLED", delivery.getNextQueryAllowedAt());
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        if (delivery.getNextQueryAllowedAt() != null && delivery.getNextQueryAllowedAt().isAfter(now)) {
            return rejected("QUERY_THROTTLED", delivery.getNextQueryAllowedAt());
        }

        List<String> pendingEventTypes = outboxService.listPendingEventTypes(deliveryId);
        if (pendingEventTypes != null && pendingEventTypes.stream().anyMatch(QUERY_EVENT_TYPES::contains)) {
            return rejected("QUERY_ALREADY_QUEUED", delivery.getNextQueryAllowedAt());
        }

        int configuredInterval = config.getMinimumQueryIntervalSeconds() == null
                ? RentalLogisticsProviderConfigService.MINIMUM_QUERY_INTERVAL_SECONDS
                : config.getMinimumQueryIntervalSeconds();
        int intervalSeconds = Math.max(
                RentalLogisticsProviderConfigService.MINIMUM_QUERY_INTERVAL_SECONDS, configuredInterval);
        LocalDateTime nextAllowedAt = now.plusSeconds(intervalSeconds);
        // The worker reserves nextQueryAllowedAt when claiming the task. Reserving it here
        // would make the newly queued task throttle itself before the provider call.
        delivery.setQueryStatus("QUEUED");
        deliveryMapper.updateById(delivery);
        outboxService.enqueue(deliveryId, RentalDeliveryOutboxEventTypeEnum.REFRESH_QUERY,
                "manual:" + clock.instant().getEpochSecond(), "manual local tracking refresh");
        return new RentalDeliveryRefreshResult(true, "REFRESH_QUEUED", nextAllowedAt);
    }

    private RentalDeliveryRefreshResult rejected(String reason, LocalDateTime nextAllowedAt) {
        return new RentalDeliveryRefreshResult(false, reason, nextAllowedAt);
    }
}
