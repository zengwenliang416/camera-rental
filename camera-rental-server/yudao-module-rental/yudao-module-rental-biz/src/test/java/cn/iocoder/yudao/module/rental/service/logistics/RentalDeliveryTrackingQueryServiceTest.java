package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingOrderSummaryRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryTraceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryTraceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryTrackingQueryServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalDeliveryDeviceRelMapper relationMapper = mock(RentalDeliveryDeviceRelMapper.class);
    private final RentalDeliveryTraceMapper traceMapper = mock(RentalDeliveryTraceMapper.class);
    private final RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
    private final RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
    private final RentalScheduleMapper scheduleMapper = mock(RentalScheduleMapper.class);
    private final RentalLogisticsRiskService riskService = new RentalLogisticsRiskService();
    private final RentalDeliveryTrackingQueryService service = new RentalDeliveryTrackingQueryService(
            deliveryMapper, relationMapper, traceMapper, orderMapper, deviceMapper, scheduleMapper,
            new WaybillPrivacy(), new SensitiveValueRedactor(), riskService,
            Clock.fixed(Instant.parse("2026-07-31T04:00:00Z"), BUSINESS_ZONE));

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void loadsAllVisibleOrderSummariesWithConstantBatchQueriesAndMaskedData() {
        RentalDeliveryDO first = delivery(20L, 10L, "SF1234567890", "IN_TRANSIT");
        first.setSourceCarrierName("顺丰速运");
        first.setLatestTraceText("Arrived 19900000000 at SF1234567890");
        RentalDeliveryDO second = delivery(21L, 10L, "JT9876543210", "DELIVERED");
        when(deliveryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(first, second));
        when(orderMapper.selectByIds(any())).thenReturn(List.of(RentalOrderDO.builder().id(10L).build()));
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                RentalDeliveryDeviceRelDO.builder().deliveryId(20L).deviceId(101L).build(),
                RentalDeliveryDeviceRelDO.builder().deliveryId(21L).deviceId(102L).build()));
        when(deviceMapper.selectByIds(any())).thenReturn(List.of(
                RentalDeviceDO.builder().id(101L).deviceNo("P4P-01").equipmentModelCode("P4P").build(),
                RentalDeviceDO.builder().id(102L).deviceNo("LENS-01").equipmentModelCode("LENS").build()));
        when(scheduleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        Map<Long, RentalDeliveryTrackingOrderSummaryRespVO> result =
                service.getSummaries(List.of(10L, 11L));

        assertEquals(2, result.size());
        assertEquals(2, result.get(10L).getPackageCount());
        assertEquals(Map.of("IN_TRANSIT", 1, "DELIVERED", 1), result.get(10L).getStatusCounts());
        assertEquals("SF1****7890", result.get(10L).getPackages().get(0).getMaskedWaybillNo());
        assertFalse(result.get(10L).getPackages().get(0).getLatestTraceText().contains("19900000000"));
        assertEquals(0, result.get(11L).getPackageCount());
        verify(deliveryMapper, times(1)).selectList(any(Wrapper.class));
        verify(relationMapper, times(1)).selectList(any(Wrapper.class));
        verify(orderMapper, times(1)).selectByIds(any());
        verify(deviceMapper, times(1)).selectByIds(any());
        verify(scheduleMapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsDevicesAndCurrentSnapshotOnlyWhenDetailIsRequested() {
        RentalDeliveryDO delivery = delivery(20L, 10L, "SF1234567890", "IN_TRANSIT");
        delivery.setTrackingVersion(3);
        when(deliveryMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(delivery);
        when(deliveryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(delivery));
        when(orderMapper.selectById(10L)).thenReturn(RentalOrderDO.builder().id(10L).build());
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                RentalDeliveryDeviceRelDO.builder().deliveryId(20L).deviceId(101L).build()));
        when(deviceMapper.selectByIds(any())).thenReturn(List.of(
                RentalDeviceDO.builder().id(101L).deviceNo("P4P-01").equipmentModelCode("P4P").build()));
        when(scheduleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(traceMapper.selectSnapshot(9L, 20L, 3)).thenReturn(List.of(
                RentalDeliveryTraceDO.builder().eventSeq(1)
                        .businessTime(LocalDateTime.of(2026, 7, 31, 9, 0))
                        .trackingStatus("PICKED_UP").traceText("picked up").build(),
                RentalDeliveryTraceDO.builder().eventSeq(2)
                        .businessTime(LocalDateTime.of(2026, 7, 31, 10, 0))
                        .trackingStatus("IN_TRANSIT").traceText("moving").build()));

        RentalDeliveryTrackingDetailRespVO result = service.getDetail(20L);

        assertEquals(20L, result.getDeliveryId());
        assertEquals("SF1****7890", result.getMaskedWaybillNo());
        assertEquals(List.of("P4P-01"),
                result.getDevices().stream().map(device -> device.getDeviceNo()).toList());
        assertEquals(List.of("IN_TRANSIT", "PICKED_UP"),
                result.getTraces().stream().map(trace -> trace.getTrackingStatus()).toList());
        verify(traceMapper).selectSnapshot(9L, 20L, 3);
    }

    @Test
    void hidesCrossTenantOrMissingDeliveryAsNotFound() {
        when(deliveryMapper.selectByTenantIdAndId(9L, 99L)).thenReturn(null);

        RentalDeliveryTrackingDetailRespVO result = service.getDetail(99L);

        assertNull(result);
    }

    private RentalDeliveryDO delivery(Long id, Long orderId, String waybill, String status) {
        return RentalDeliveryDO.builder()
                .id(id)
                .rentalOrderId(orderId)
                .direction("OUTBOUND")
                .packageSeq(1)
                .waybillNo(waybill)
                .mappingStatus("READY")
                .subscribeStatus("SUBSCRIBED")
                .queryStatus("READY")
                .trackingStatus(status)
                .lastSyncedAt(LocalDateTime.of(2026, 7, 31, 10, 0))
                .build();
    }
}
