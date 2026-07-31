package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingSnapshot;

public interface RentalTrackingSnapshotService {

    boolean apply(Long deliveryId, LogisticsTrackingSnapshot snapshot);
}
