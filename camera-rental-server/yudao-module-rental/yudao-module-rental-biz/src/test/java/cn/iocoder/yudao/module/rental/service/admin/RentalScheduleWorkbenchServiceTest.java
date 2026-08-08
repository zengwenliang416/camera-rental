package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleWorkbenchMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalScheduleWorkbenchServiceTest {

    private RentalScheduleWorkbenchMapper workbenchMapper;
    private RentalDeviceAssignmentMapper assignmentMapper;
    private RentalDeliveryDeviceRelMapper deliveryRelationMapper;
    private RentalDeliveryMapper deliveryMapper;
    private RentalManualReviewMapper manualReviewMapper;
    private RentalOrderItemMapper orderItemMapper;
    private RentalOrderMapper orderMapper;
    private RentalScheduleMapper scheduleMapper;
    private RentalScheduleWorkbenchService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        workbenchMapper = mock(RentalScheduleWorkbenchMapper.class);
        assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        deliveryRelationMapper = mock(RentalDeliveryDeviceRelMapper.class);
        deliveryMapper = mock(RentalDeliveryMapper.class);
        manualReviewMapper = mock(RentalManualReviewMapper.class);
        orderItemMapper = mock(RentalOrderItemMapper.class);
        orderMapper = mock(RentalOrderMapper.class);
        scheduleMapper = mock(RentalScheduleMapper.class);
        service = new RentalScheduleWorkbenchService(workbenchMapper, assignmentMapper, deliveryRelationMapper,
                deliveryMapper, manualReviewMapper, orderItemMapper, orderMapper, scheduleMapper,
                Clock.fixed(Instant.parse("2026-08-08T00:00:00Z"), ZoneId.of("Asia/Shanghai")));
        when(scheduleMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.selectList(any())).thenReturn(List.of());
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        when(assignmentMapper.selectList(any())).thenReturn(List.of());
        when(deliveryRelationMapper.selectList(any())).thenReturn(List.of());
        when(deliveryMapper.selectList(any())).thenReturn(List.of());
        when(manualReviewMapper.selectList(any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPageDevicesAndClipHalfOpenSegmentsWithContinuationMarkers() {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(101L).deviceNo("A7M4-0001").serialNumber("SN-001")
                .equipmentModelCode("SONY-A7M4").status("AVAILABLE").enabled(true).build();
        Page<RentalDeviceDO> page = new Page<>(2, 25);
        page.setRecords(List.of(device));
        page.setTotal(51);
        when(workbenchMapper.selectDevicePage(any(), eq(9L), any(), any(), any(), any())).thenReturn(page);
        when(workbenchMapper.selectDeviceMetrics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        when(scheduleMapper.selectList(any())).thenReturn(List.of(
                schedule(1L, 101L, LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 16)),
                schedule(2L, 101L, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 1)),
                schedule(3L, 101L, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 20))));

        RentalScheduleWorkbenchReqVO req = request(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15), "14D");
        req.setPageNo(2);
        req.setPageSize(25);

        RentalScheduleWorkbenchRespVO result = service.getWorkbench(req);

        assertEquals(51L, result.getDevicePage().getTotal());
        assertEquals(1, result.getDevicePage().getList().size());
        assertEquals(1, result.getDevicePage().getList().get(0).getSegments().size());
        var segment = result.getDevicePage().getList().get(0).getSegments().get(0);
        assertEquals(LocalDate.of(2026, 7, 31), segment.getOccupyStartDate());
        assertEquals(LocalDate.of(2026, 8, 16), segment.getOccupyEndDateExclusive());
        assertEquals(LocalDate.of(2026, 8, 1), segment.getDisplayStartDate());
        assertEquals(LocalDate.of(2026, 8, 15), segment.getDisplayEndDateExclusive());
        assertTrue(segment.getLeftContinuation());
        assertTrue(segment.getRightContinuation());
        verify(workbenchMapper).selectDevicePage(any(), eq(9L), any(), any(), any(), any());
        verify(scheduleMapper).selectList(any());
    }

    @Test
    void shouldKeepReturnedDeviceOccupiedUntilInspectionAndExposePendingAllocation() {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(201L).deviceNo("A7M4-0002").equipmentModelCode("SONY-A7M4")
                .status("RENTED").enabled(true).build();
        Page<RentalDeviceDO> page = new Page<>(1, 25);
        page.setRecords(List.of(device));
        page.setTotal(1);
        when(workbenchMapper.selectDevicePage(any(), eq(9L), any(), any(), any(), any())).thenReturn(page);
        when(workbenchMapper.selectDeviceMetrics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        when(deliveryRelationMapper.selectList(any())).thenReturn(List.of(
                RentalDeliveryDeviceRelDO.builder().deliveryId(301L).deviceId(201L).build()));
        when(deliveryMapper.selectList(any())).thenReturn(List.of(RentalDeliveryDO.builder()
                .id(301L).rentalOrderId(401L).direction("RETURN").trackingStatus("RETURNED")
                .lifecycleStatus("ACTIVE").build()));
        when(orderMapper.selectList(any())).thenReturn(List.of(RentalOrderDO.builder()
                .id(501L).orderNo("R-501").status("PENDING_ALLOCATION").build()));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(RentalOrderItemDO.builder()
                .id(502L).rentalOrderId(501L).equipmentModelCode("SONY-A7M4")
                .quantity(2).occupyStartDate(LocalDate.of(2026, 8, 10))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 13)).build()));
        when(assignmentMapper.selectList(any())).thenReturn(List.of(RentalDeviceAssignmentDO.builder()
                .id(503L).rentalOrderId(501L).rentalOrderItemId(502L).deviceId(201L)
                .status("ASSIGNED").build()));

        RentalScheduleWorkbenchRespVO result = service.getWorkbench(
                request(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15), "14D"));

        var lane = result.getDevicePage().getList().get(0);
        assertEquals("RETURNED_PENDING_INSPECTION", lane.getLogisticsStatus());
        assertTrue(lane.getOccupied());
        assertEquals(1, result.getExceptions().stream()
                .filter(item -> "RETURN_INSPECTION_PENDING".equals(item.getCode())).count());
        assertEquals(1, result.getPendingAllocations().size());
        assertEquals(2, result.getPendingAllocations().get(0).getRequiredQuantity());
        assertEquals(1, result.getPendingAllocations().get(0).getAssignedQuantity());
        assertEquals(1, result.getPendingAllocations().get(0).getRemainingQuantity());
    }

    private RentalScheduleWorkbenchReqVO request(LocalDate from, LocalDate toExclusive, String viewMode) {
        RentalScheduleWorkbenchReqVO req = new RentalScheduleWorkbenchReqVO();
        req.setFromDate(from);
        req.setToDateExclusive(toExclusive);
        req.setViewMode(viewMode);
        req.setPageNo(1);
        req.setPageSize(25);
        return req;
    }

    private RentalScheduleDO schedule(Long id, Long deviceId, LocalDate start, LocalDate endExclusive) {
        return RentalScheduleDO.builder().id(id).deviceId(deviceId).status("EFFECTIVE")
                .scheduleType("RENTAL").occupyStartDate(start).occupyEndDateExclusive(endExclusive).build();
    }
}
