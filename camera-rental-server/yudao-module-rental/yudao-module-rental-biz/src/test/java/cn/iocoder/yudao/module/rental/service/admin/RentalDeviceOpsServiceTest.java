package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceReturnReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceUnassignReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.enums.rental.RentalDeviceLockTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DISPATCH_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_RETURN_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_UNASSIGN_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeviceOpsServiceTest {

    private final RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper =
            mock(RentalDeviceAssignmentMapper.class);
    private final RentalScheduleMapper scheduleMapper = mock(RentalScheduleMapper.class);
    private final RentalDeviceLockService lockService = mock(RentalDeviceLockService.class);
    private final RentalDeviceOpsService service =
            new RentalDeviceOpsService(deviceMapper, assignmentMapper, scheduleMapper, lockService);

    @Test
    void dispatchThenReturnPass() {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("AVAILABLE").enabled(true).build();
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("ASSIGNED").scheduleId(77L).build();
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(assignment);

        RentalDeviceDispatchReqVO dispatchReq = new RentalDeviceDispatchReqVO();
        dispatchReq.setDeviceId(1L);
        RentalDeviceOpsRespVO dispatched = service.dispatch(dispatchReq);
        assertEquals("RENTED", dispatched.getDeviceStatus());
        assertEquals("DISPATCHED", dispatched.getAssignmentStatus());

        device.setStatus("RENTED");
        assignment.setStatus("DISPATCHED");
        RentalDeviceReturnReqVO returnReq = new RentalDeviceReturnReqVO();
        returnReq.setDeviceId(1L);
        returnReq.setInspectPassed(true);
        RentalDeviceOpsRespVO returned = service.returnDevice(returnReq);
        assertEquals("AVAILABLE", returned.getDeviceStatus());
        assertEquals("RETURNED", returned.getAssignmentStatus());
        verify(lockService).releaseSystemLockForLockedDevice(1L,
                RentalDeviceLockTypeEnum.RETURN_INSPECTION, "INSPECTION_COMPLETED");

        ArgumentCaptor<RentalDeviceDO> deviceCap = ArgumentCaptor.forClass(RentalDeviceDO.class);
        verify(deviceMapper, org.mockito.Mockito.atLeastOnce()).updateById(deviceCap.capture());
    }

    @Test
    void returnNarrowsScheduleToCheckInDay() {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("RENTED").enabled(true).build();
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("DISPATCHED").scheduleId(77L).build();
        RentalScheduleDO schedule = RentalScheduleDO.builder()
                .id(77L).deviceId(1L).status("EFFECTIVE")
                .occupyStartDate(LocalDate.now().minusDays(3))
                .occupyEndDateExclusive(LocalDate.now().plusDays(20)).build();
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(assignment);
        when(scheduleMapper.selectByIdForUpdate(77L)).thenReturn(schedule);

        RentalDeviceReturnReqVO returnReq = new RentalDeviceReturnReqVO();
        returnReq.setDeviceId(1L);
        returnReq.setNote("外观完好");
        RentalDeviceOpsRespVO returned = service.returnDevice(returnReq);

        assertEquals("RETURNED", returned.getAssignmentStatus());
        assertEquals(LocalDate.now().plusDays(1), schedule.getOccupyEndDateExclusive());
        verify(scheduleMapper).updateById(schedule);
        assertNotNull(assignment.getReturnedAt());
        assertEquals("外观完好", assignment.getReturnNote());
    }

    @Test
    void returnKeepsScheduleWhenAlreadyEnded() {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("RENTED").enabled(true).build();
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("DISPATCHED").scheduleId(77L).build();
        RentalScheduleDO schedule = RentalScheduleDO.builder()
                .id(77L).deviceId(1L).status("EFFECTIVE")
                .occupyStartDate(LocalDate.now().minusDays(10))
                .occupyEndDateExclusive(LocalDate.now()).build();
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(assignment);
        when(scheduleMapper.selectByIdForUpdate(77L)).thenReturn(schedule);

        RentalDeviceReturnReqVO returnReq = new RentalDeviceReturnReqVO();
        returnReq.setDeviceId(1L);
        service.returnDevice(returnReq);

        assertEquals(LocalDate.now(), schedule.getOccupyEndDateExclusive());
        verify(scheduleMapper, never()).updateById(any(RentalScheduleDO.class));
    }

    @Test
    void returnByDeviceNoResolvesDevice() {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("RENTED").enabled(true).build();
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("DISPATCHED").build();
        when(deviceMapper.selectByDeviceNoForUpdate("A7M4-0001")).thenReturn(device);
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(assignment);

        RentalDeviceReturnReqVO returnReq = new RentalDeviceReturnReqVO();
        returnReq.setDeviceNo("A7M4-0001");
        RentalDeviceOpsRespVO returned = service.returnDevice(returnReq);

        assertEquals("AVAILABLE", returned.getDeviceStatus());
    }

    @Test
    void returnFailGoesMaintenance() {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("RENTED").enabled(true).build();
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).rentalOrderId(30L).rentalOrderItemId(40L)
                .status("DISPATCHED").build();
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(assignment);

        RentalDeviceReturnReqVO returnReq = new RentalDeviceReturnReqVO();
        returnReq.setDeviceId(1L);
        returnReq.setInspectPassed(false);
        RentalDeviceOpsRespVO returned = service.returnDevice(returnReq);
        assertEquals("MAINTENANCE", returned.getDeviceStatus());
        assertEquals("RETURNED", returned.getAssignmentStatus());
        verify(lockService).createSystemLockForLockedDevice(1L, RentalDeviceLockTypeEnum.MAINTENANCE,
                "INSPECTION_FAILED", assignment.getRentalOrderId(), assignment.getRentalOrderItemId());
    }

    @Test
    void unassignCancelsScheduleAndAssignment() {
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("ASSIGNED").scheduleId(77L).build();
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("AVAILABLE").enabled(true).build();
        RentalScheduleDO schedule = RentalScheduleDO.builder()
                .id(77L).deviceId(1L).status("EFFECTIVE")
                .occupyStartDate(LocalDate.now().plusDays(1))
                .occupyEndDateExclusive(LocalDate.now().plusDays(5)).build();
        when(assignmentMapper.selectByIdForUpdate(9L)).thenReturn(assignment);
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);
        when(scheduleMapper.selectByIdForUpdate(77L)).thenReturn(schedule);

        RentalDeviceUnassignReqVO unassignReq = new RentalDeviceUnassignReqVO();
        unassignReq.setAssignmentId(9L);
        RentalDeviceOpsRespVO result = service.unassign(unassignReq);

        assertEquals("CANCELED", result.getAssignmentStatus());
        assertEquals("CANCELED", schedule.getStatus());
        verify(scheduleMapper).updateById(schedule);
        verify(assignmentMapper).updateById(assignment);
    }

    @Test
    void unassignDispatchedAssignmentFails() {
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("DISPATCHED").build();
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("RENTED").enabled(true).build();
        when(assignmentMapper.selectByIdForUpdate(9L)).thenReturn(assignment);
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);

        RentalDeviceUnassignReqVO unassignReq = new RentalDeviceUnassignReqVO();
        unassignReq.setAssignmentId(9L);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.unassign(unassignReq));
        assertEquals(RENTAL_DEVICE_UNASSIGN_FAILED.getCode(), ex.getCode());
        verify(assignmentMapper, never()).updateById(any(RentalDeviceAssignmentDO.class));
    }

    @Test
    void dispatchWithoutAssignmentFails() {
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(RentalDeviceDO.builder()
                .id(1L).status("AVAILABLE").enabled(true).build());
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(null);

        RentalDeviceDispatchReqVO dispatchReq = new RentalDeviceDispatchReqVO();
        dispatchReq.setDeviceId(1L);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.dispatch(dispatchReq));
        assertEquals(RENTAL_DEVICE_DISPATCH_FAILED.getCode(), ex.getCode());
    }

    @Test
    void returnWhenNotRentedFails() {
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(RentalDeviceDO.builder()
                .id(1L).status("AVAILABLE").enabled(true).build());

        RentalDeviceReturnReqVO returnReq = new RentalDeviceReturnReqVO();
        returnReq.setDeviceId(1L);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.returnDevice(returnReq));
        assertEquals(RENTAL_DEVICE_RETURN_FAILED.getCode(), ex.getCode());
    }

}
