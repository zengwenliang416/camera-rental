package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalPendingAllocationOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalPendingAllocationPageReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleAllocationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RentalScheduleAllocationServiceTest {

    private RentalScheduleAllocationMapper allocationMapper;
    private RentalOrderItemMapper orderItemMapper;
    private RentalDeviceAssignmentMapper assignmentMapper;
    private XianyuOrderMapper xianyuOrderMapper;
    private RentalScheduleAllocationService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        allocationMapper = mock(RentalScheduleAllocationMapper.class);
        RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
        orderItemMapper = mock(RentalOrderItemMapper.class);
        xianyuOrderMapper = mock(XianyuOrderMapper.class);
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        RentalScheduleMapper scheduleMapper = mock(RentalScheduleMapper.class);
        RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
        RentalDeliveryDeviceRelMapper deliveryRelationMapper = mock(RentalDeliveryDeviceRelMapper.class);
        service = new RentalScheduleAllocationService(allocationMapper, orderMapper, orderItemMapper,
                xianyuOrderMapper, deviceMapper, assignmentMapper, scheduleMapper, deliveryMapper,
                deliveryRelationMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldExposeRealExternalOrderNumberForPendingAllocation() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(501L)
                .orderNo("XY-0000000000000000501")
                .channelOrderId(601L)
                .sourceType("XIANYU")
                .sourceOrderId("7:3892746501234567890")
                .status("PENDING_ALLOCATION")
                .billableStartDate(LocalDate.of(2026, 8, 10))
                .billableEndDate(LocalDate.of(2026, 8, 12))
                .build();
        order.setTenantId(9L);
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .id(701L)
                .rentalOrderId(501L)
                .equipmentModelCode("P4P")
                .quantity(1)
                .occupyStartDate(LocalDate.of(2026, 8, 9))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 14))
                .build();
        item.setTenantId(9L);
        XianyuOrderDO channelOrder = XianyuOrderDO.builder()
                .id(601L)
                .externalOrderId("3892746501234567890")
                .build();
        channelOrder.setTenantId(9L);

        when(allocationMapper.countPendingAllocationOrders(eq(9L), any(), any())).thenReturn(1L);
        when(allocationMapper.selectPendingAllocationOrders(eq(9L), any(), any(), eq(0L), eq(10)))
                .thenReturn(List.of(order));
        when(orderItemMapper.selectListByRentalOrderIds(List.of(501L))).thenReturn(List.of(item));
        when(assignmentMapper.selectActiveListByRentalOrderIds(List.of(501L))).thenReturn(List.of());
        when(xianyuOrderMapper.selectByIds(List.of(601L))).thenReturn(List.of(channelOrder));

        RentalPendingAllocationPageReqVO request = new RentalPendingAllocationPageReqVO();
        request.setPageNo(1);
        request.setPageSize(10);
        PageResult<RentalPendingAllocationOrderRespVO> result = service.getPendingAllocationPage(request);

        assertEquals(1L, result.getTotal());
        assertEquals("3892746501234567890", result.getList().get(0).getExternalOrderNo());
        assertEquals("XY-0000000000000000501", result.getList().get(0).getOrderNo());
    }
}
