package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalAsyncProcessingStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RentalDeliveryInboxLeaseService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final RentalDeliveryCallbackInboxMapper inboxMapper;

    public RentalDeliveryInboxLeaseService(RentalDeliveryCallbackInboxMapper inboxMapper) {
        this.inboxMapper = inboxMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<RentalInboxWorkItem> claim(int requestedLimit) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        List<RentalDeliveryCallbackInboxDO> inboxes = inboxMapper.selectClaimableForUpdate(
                tenantId, now, Math.max(1, Math.min(requestedLimit, 100)));
        List<RentalInboxWorkItem> work = new ArrayList<>(inboxes.size());
        for (RentalDeliveryCallbackInboxDO inbox : inboxes) {
            String token = UUID.randomUUID().toString();
            inbox.setProcessingStatus(RentalAsyncProcessingStatusEnum.PROCESSING.name());
            inbox.setProcessingToken(token);
            inbox.setLeaseUntil(now.plusMinutes(5));
            inboxMapper.updateById(inbox);
            work.add(new RentalInboxWorkItem(tenantId, inbox.getId(), token, inbox.getDeliveryId(),
                    inbox.getProviderCode(), inbox.getCallbackParams()));
        }
        return work;
    }
}
