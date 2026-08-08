package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceReturnReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.enums.rental.RentalDeviceLockTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DISPATCH_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_RETURN_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeviceOpsServiceTest {

    @Test
    void dispatchThenReturnPass() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        RentalDeviceLockService lockService = mock(RentalDeviceLockService.class);
        RentalDeviceOpsService service = new RentalDeviceOpsService(deviceMapper, assignmentMapper, lockService);

        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("AVAILABLE").enabled(true).build();
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("ASSIGNED").build();
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(assignment);

        RentalDeviceDispatchReqVO dispatchReq = new RentalDeviceDispatchReqVO();
        dispatchReq.setDeviceId(1L);
        RentalDeviceOpsRespVO dispatched = service.dispatch(dispatchReq);
        assertEquals("RENTED", dispatched.getDeviceStatus());
        assertEquals("DISPATCHED", dispatched.getAssignmentStatus());

        device.setStatus("RENTED");
        assignment.setStatus("DISPATCHED");
        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(device);
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(1L)).thenReturn(assignment);

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
    void returnFailGoesMaintenance() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        RentalDeviceLockService lockService = mock(RentalDeviceLockService.class);
        RentalDeviceOpsService service = new RentalDeviceOpsService(deviceMapper, assignmentMapper, lockService);

        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(1L).deviceNo("A7M4-0001").status("RENTED").enabled(true).build();
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(9L).deviceId(1L).status("DISPATCHED").build();
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
    void dispatchWithoutAssignmentFails() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        RentalDeviceOpsService service = new RentalDeviceOpsService(deviceMapper, assignmentMapper,
                mock(RentalDeviceLockService.class));

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
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        RentalDeviceOpsService service = new RentalDeviceOpsService(deviceMapper, assignmentMapper,
                mock(RentalDeviceLockService.class));

        when(deviceMapper.selectByIdForUpdate(1L)).thenReturn(RentalDeviceDO.builder()
                .id(1L).status("AVAILABLE").enabled(true).build());

        RentalDeviceReturnReqVO returnReq = new RentalDeviceReturnReqVO();
        returnReq.setDeviceId(1L);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.returnDevice(returnReq));
        assertEquals(RENTAL_DEVICE_RETURN_FAILED.getCode(), ex.getCode());
    }

}
