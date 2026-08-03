package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalAsyncProcessingStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class RentalDeliveryOutboxServiceImpl implements RentalDeliveryOutboxService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String SYSTEM_OPERATOR = "system";
    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryOutboxMapper outboxMapper;
    private final SensitiveValueRedactor redactor;

    public RentalDeliveryOutboxServiceImpl(RentalDeliveryMapper deliveryMapper,
                                           RentalDeliveryOutboxMapper outboxMapper,
                                           SensitiveValueRedactor redactor) {
        this.deliveryMapper = deliveryMapper;
        this.outboxMapper = outboxMapper;
        this.redactor = redactor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long enqueue(Long deliveryId, RentalDeliveryOutboxEventTypeEnum eventType, String dedupeSuffix,
                        String safeMetadata) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndId(tenantId, deliveryId);
        if (delivery == null) {
            throw new RentalLogisticsException("DELIVERY_NOT_FOUND");
        }
        String suffix = StringUtils.hasText(dedupeSuffix) ? ":" + dedupeSuffix.trim() : "";
        String dedupeKey = "delivery:" + deliveryId + ":" + eventType.name() + suffix;
        RentalDeliveryOutboxDO outbox = RentalDeliveryOutboxDO.builder()
                .deliveryId(deliveryId)
                .eventType(eventType.name())
                .dedupeKey(dedupeKey)
                .safeMetadata(redactor.redact(safeMetadata))
                .processingStatus(RentalAsyncProcessingStatusEnum.PENDING.name())
                .retryCount(0)
                .scheduledAt(LocalDateTime.now(BUSINESS_ZONE))
                .build();
        outbox.setCreator(SYSTEM_OPERATOR);
        outbox.setUpdater(SYSTEM_OPERATOR);
        outboxMapper.insertOrReuse(tenantId, outbox);
        RentalDeliveryOutboxDO persisted = outboxMapper.selectByDedupeKeyForUpdate(tenantId, dedupeKey);
        if (persisted == null) {
            throw new IllegalStateException("Delivery outbox disappeared after insert");
        }
        return persisted.getId();
    }

    @Override
    public List<String> listPendingEventTypes(Long deliveryId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (deliveryMapper.selectByTenantIdAndId(tenantId, deliveryId) == null) {
            throw new RentalLogisticsException("DELIVERY_NOT_FOUND");
        }
        return outboxMapper.selectPendingByDeliveryId(tenantId, deliveryId).stream()
                .map(RentalDeliveryOutboxDO::getEventType)
                .toList();
    }
}
