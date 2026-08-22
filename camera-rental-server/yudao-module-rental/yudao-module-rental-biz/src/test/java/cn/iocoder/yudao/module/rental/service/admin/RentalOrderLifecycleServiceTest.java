package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_ORDER_CANCEL_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalOrderLifecycleServiceTest {

    private final RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper =
            mock(RentalDeviceAssignmentMapper.class);
    private final RentalScheduleMapper scheduleMapper = mock(RentalScheduleMapper.class);
    private final RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
    private final RentalOrderLifecycleService service =
            new RentalOrderLifecycleService(orderMapper, assignmentMapper, scheduleMapper, deviceMapper);

    private RentalOrderDO order;

    @BeforeEach
    void setUp() {
        order = RentalOrderDO.builder()
                .id(30L).orderNo("RO-001").status("PENDING_ALLOCATION").build();
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(order);
    }

    @Test
    void cancelOrderReleasesAssignedAllocationsAndSchedules() {
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).rentalOrderId(30L).deviceId(1L).status("ASSIGNED").scheduleId(77L).build();
        RentalScheduleDO schedule = RentalScheduleDO.builder()
                .id(77L).deviceId(1L).status("EFFECTIVE")
                .occupyStartDate(LocalDate.now().plusDays(1))
                .occupyEndDateExclusive(LocalDate.now().plusDays(5)).build();
        when(assignmentMapper.selectActiveListByRentalOrderId(30L)).thenReturn(List.of(assignment));
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("AVAILABLE").build());
        when(scheduleMapper.selectByIdForUpdate(77L)).thenReturn(schedule);

        service.cancelOrder(30L, "客户改期");

        assertEquals("CANCELED", order.getStatus());
        assertEquals("客户改期", order.getCancelReason());
        assertEquals("CANCELED", assignment.getStatus());
        assertEquals("CANCELED", schedule.getStatus());
        verify(scheduleMapper).updateById(schedule);
        verify(assignmentMapper).updateById(assignment);
        verify(orderMapper).updateById(order);
    }

    @Test
    void cancelOrderBlockedWhileDeviceDispatched() {
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).rentalOrderId(30L).deviceId(1L).status("DISPATCHED").build();
        when(assignmentMapper.selectActiveListByRentalOrderId(30L)).thenReturn(List.of(assignment));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.cancelOrder(30L, null));
        assertEquals(RENTAL_ORDER_CANCEL_FAILED.getCode(), ex.getCode());
        assertEquals("PENDING_ALLOCATION", order.getStatus());
        verify(assignmentMapper, never()).updateById(any(RentalDeviceAssignmentDO.class));
        verify(orderMapper, never()).updateById(any(RentalOrderDO.class));
    }

    @Test
    void cancelOrderIsIdempotent() {
        order.setStatus("CANCELED");
        order.setCancelReason("已取消");

        service.cancelOrder(30L, "重复请求");

        assertEquals("已取消", order.getCancelReason());
        verify(orderMapper, never()).updateById(any(RentalOrderDO.class));
        verify(assignmentMapper, never()).selectActiveListByRentalOrderId(30L);
    }

    @Test
    void cancelMissingOrderFails() {
        when(orderMapper.selectByIdForUpdate(99L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.cancelOrder(99L, null));
        assertEquals(RENTAL_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void cancelOrderWithoutAssignmentsJustMarksOrder() {
        when(assignmentMapper.selectActiveListByRentalOrderId(30L)).thenReturn(List.of());

        service.cancelOrder(30L, "  ");

        assertEquals("CANCELED", order.getStatus());
        assertNull(order.getCancelReason());
        verify(orderMapper).updateById(order);
    }

}
