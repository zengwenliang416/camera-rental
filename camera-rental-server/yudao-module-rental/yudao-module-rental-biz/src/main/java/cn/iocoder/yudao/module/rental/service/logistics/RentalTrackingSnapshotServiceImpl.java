package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryTraceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryTraceMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class RentalTrackingSnapshotServiceImpl implements RentalTrackingSnapshotService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryTraceMapper traceMapper;
    private final TrackingSnapshotNormalizer normalizer;

    public RentalTrackingSnapshotServiceImpl(RentalDeliveryMapper deliveryMapper,
                                             RentalDeliveryTraceMapper traceMapper,
                                             TrackingSnapshotNormalizer normalizer) {
        this.deliveryMapper = deliveryMapper;
        this.traceMapper = traceMapper;
        this.normalizer = normalizer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean apply(Long deliveryId, LogisticsTrackingSnapshot snapshot) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndIdForUpdate(tenantId, deliveryId);
        if (delivery == null) {
            throw new RentalLogisticsException("DELIVERY_NOT_FOUND");
        }
        NormalizedTrackingSnapshot normalized = normalizer.normalize(snapshot);
        LocalDateTime synchronizedAt = normalized.synchronizedAt() == null
                ? LocalDateTime.now(BUSINESS_ZONE) : normalized.synchronizedAt();
        if (normalized.events().isEmpty()) {
            delivery.setLastSyncedAt(synchronizedAt);
            deliveryMapper.updateById(delivery);
            return false;
        }
        if (Objects.equals(delivery.getCurrentSnapshotHash(), normalized.snapshotHash())) {
            delivery.setLastSyncedAt(synchronizedAt);
            deliveryMapper.updateById(delivery);
            return false;
        }

        int nextVersion = (delivery.getTrackingVersion() == null ? 0 : delivery.getTrackingVersion()) + 1;
        for (NormalizedTrackingEvent event : normalized.events()) {
            traceMapper.insert(toTrace(deliveryId, nextVersion, normalized.snapshotHash(), event));
        }
        NormalizedTrackingEvent latest = latest(normalized.events());
        RentalTrackingStatusEnum currentStatus = parseStatus(delivery.getTrackingStatus());
        RentalTrackingStatusEnum protectedStatus =
                normalizer.protectTerminal(currentStatus, latest.trackingStatus());
        boolean protectedFromRegression = currentStatus != null && currentStatus.isTerminal()
                && !latest.trackingStatus().isTerminal();

        delivery.setTrackingVersion(nextVersion);
        delivery.setCurrentSnapshotHash(normalized.snapshotHash());
        delivery.setLastSyncedAt(synchronizedAt);
        delivery.setEstimatedDeliveryAt(normalized.estimatedDeliveryAt());
        delivery.setTrackingStatus(protectedStatus.name());
        if (!protectedFromRegression) {
            delivery.setLatestEventTime(latest.businessTime());
            delivery.setLatestTraceText(latest.traceText());
            delivery.setLatestLocation(latest.location());
        }
        deliveryMapper.updateById(delivery);
        return true;
    }

    private RentalDeliveryTraceDO toTrace(Long deliveryId, int version, String snapshotHash,
                                          NormalizedTrackingEvent event) {
        return RentalDeliveryTraceDO.builder()
                .deliveryId(deliveryId)
                .snapshotVersion(version)
                .snapshotHash(snapshotHash)
                .eventSeq(event.sequence())
                .eventFingerprint(event.fingerprint())
                .businessTime(event.businessTime())
                .rawTime(event.rawTime())
                .trackingStatus(event.trackingStatus().name())
                .providerStatus(event.providerStatus())
                .traceText(event.traceText())
                .location(event.location())
                .eventSource(event.source())
                .inboxId(event.inboxId())
                .build();
    }

    private NormalizedTrackingEvent latest(List<NormalizedTrackingEvent> events) {
        return events.get(events.size() - 1);
    }

    private RentalTrackingStatusEnum parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return RentalTrackingStatusEnum.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            return RentalTrackingStatusEnum.UNKNOWN;
        }
    }
}
