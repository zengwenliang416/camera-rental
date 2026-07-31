package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryTraceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryTraceMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingEvent;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalTrackingSnapshotServiceImplTest {

    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalDeliveryTraceMapper traceMapper = mock(RentalDeliveryTraceMapper.class);
    private final TrackingSnapshotNormalizer normalizer = new TrackingSnapshotNormalizer(new LogisticsHashing());
    private final RentalTrackingSnapshotService service =
            new RentalTrackingSnapshotServiceImpl(deliveryMapper, traceMapper, normalizer);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void persistsChangedCompleteSnapshotAndAdvancesVersion() {
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(10L)
                .trackingStatus("CREATED")
                .trackingVersion(0)
                .build();
        delivery.setTenantId(9L);
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 10L)).thenReturn(delivery);

        boolean changed = service.apply(10L, snapshot(RentalTrackingStatusEnum.IN_TRANSIT, "moving"));

        assertTrue(changed);
        assertEquals(1, delivery.getTrackingVersion());
        assertEquals("IN_TRANSIT", delivery.getTrackingStatus());
        verify(traceMapper).insert(any(RentalDeliveryTraceDO.class));
        verify(deliveryMapper).updateById(delivery);
    }

    @Test
    void identicalSnapshotOnlyRefreshesSyncMetadata() {
        LogisticsTrackingSnapshot snapshot = snapshot(RentalTrackingStatusEnum.IN_TRANSIT, "moving");
        String hash = normalizer.normalize(snapshot).snapshotHash();
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(10L)
                .trackingStatus("IN_TRANSIT")
                .trackingVersion(3)
                .currentSnapshotHash(hash)
                .build();
        delivery.setTenantId(9L);
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 10L)).thenReturn(delivery);

        boolean changed = service.apply(10L, snapshot);

        assertFalse(changed);
        assertEquals(3, delivery.getTrackingVersion());
        verify(traceMapper, never()).insert(any(RentalDeliveryTraceDO.class));
        verify(deliveryMapper).updateById(delivery);
    }

    @Test
    void lateNonTerminalSnapshotCannotRegressDeliveredSummary() {
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(10L)
                .trackingStatus("DELIVERED")
                .trackingVersion(2)
                .latestTraceText("delivered")
                .latestEventTime(LocalDateTime.parse("2026-07-31T12:00:00"))
                .build();
        delivery.setTenantId(9L);
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 10L)).thenReturn(delivery);

        service.apply(10L, snapshot(RentalTrackingStatusEnum.IN_TRANSIT, "late old event"));

        assertEquals("DELIVERED", delivery.getTrackingStatus());
        assertEquals("delivered", delivery.getLatestTraceText());
        assertEquals(3, delivery.getTrackingVersion());
        ArgumentCaptor<RentalDeliveryTraceDO> captor = ArgumentCaptor.forClass(RentalDeliveryTraceDO.class);
        verify(traceMapper).insert(captor.capture());
        assertEquals("IN_TRANSIT", captor.getValue().getTrackingStatus());
    }

    private LogisticsTrackingSnapshot snapshot(RentalTrackingStatusEnum status, String text) {
        LocalDateTime time = LocalDateTime.parse("2026-07-31T10:00:00");
        return new LogisticsTrackingSnapshot(List.of(
                new LogisticsTrackingEvent(time, time.toString(), status, status.name(), text,
                        "Changsha", "QUERY", null)), null, time.plusMinutes(1));
    }
}
