package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RentalLogisticsCompensationService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DEDUPE_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryOutboxService outboxService;

    public RentalLogisticsCompensationService(RentalDeliveryMapper deliveryMapper,
                                              RentalDeliveryOutboxService outboxService) {
        this.deliveryMapper = deliveryMapper;
        this.outboxService = outboxService;
    }

    public int enqueueStale(int limit) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<RentalDeliveryDO> deliveries = deliveryMapper.selectCompensationCandidates(
                tenantId, now.minusHours(6), limit);
        String bucket = DEDUPE_BUCKET.format(now);
        for (RentalDeliveryDO delivery : deliveries) {
            outboxService.enqueue(delivery.getId(), RentalDeliveryOutboxEventTypeEnum.RECONCILE,
                    bucket, "stale tracking compensation");
        }
        return deliveries.size();
    }
}
