package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;

import java.util.List;

public interface RentalDeliveryOutboxService {

    Long enqueue(Long deliveryId, RentalDeliveryOutboxEventTypeEnum eventType, String dedupeSuffix,
                 String safeMetadata);

    List<String> listPendingEventTypes(Long deliveryId);
}
