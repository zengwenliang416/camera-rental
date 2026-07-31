package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalAsyncProcessingStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class RentalDeliveryInboxServiceImpl implements RentalDeliveryInboxService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryCallbackInboxMapper inboxMapper;

    public RentalDeliveryInboxServiceImpl(RentalDeliveryMapper deliveryMapper,
                                          RentalDeliveryCallbackInboxMapper inboxMapper) {
        this.deliveryMapper = deliveryMapper;
        this.inboxMapper = inboxMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long accept(String providerCode, Long deliveryId, String providerTaskId, String payloadHash,
                       String callbackParams) {
        if (!StringUtils.hasText(providerCode) || !StringUtils.hasText(payloadHash)
                || !StringUtils.hasText(callbackParams)) {
            throw new RentalLogisticsException("INBOX_PAYLOAD_INVALID");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndId(tenantId, deliveryId);
        if (delivery == null) {
            throw new RentalLogisticsException("DELIVERY_NOT_FOUND");
        }
        LocalDateTime receivedAt = LocalDateTime.now(BUSINESS_ZONE);
        delivery.setLastCallbackAt(receivedAt);
        deliveryMapper.updateById(delivery);
        RentalDeliveryCallbackInboxDO inbox = RentalDeliveryCallbackInboxDO.builder()
                .providerCode(providerCode)
                .deliveryId(deliveryId)
                .providerTaskId(providerTaskId)
                .payloadHash(payloadHash)
                .callbackParams(callbackParams)
                .processingStatus(RentalAsyncProcessingStatusEnum.RECEIVED.name())
                .retryCount(0)
                .receivedAt(receivedAt)
                .build();
        inboxMapper.insertOrReuse(tenantId, inbox);
        RentalDeliveryCallbackInboxDO persisted =
                inboxMapper.selectByPayloadHashForUpdate(tenantId, providerCode, deliveryId, payloadHash);
        if (persisted == null) {
            throw new IllegalStateException("Callback inbox disappeared after insert");
        }
        return persisted.getId();
    }
}
