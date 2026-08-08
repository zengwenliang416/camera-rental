package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLockCreateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceLockMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_LOCK_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeviceLockServiceTest {

    @Test
    void createsAuditedManualOrderHold() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceLockMapper lockMapper = mock(RentalDeviceLockMapper.class);
        RentalDeviceLockService service = new RentalDeviceLockService(deviceMapper, lockMapper);
        when(deviceMapper.selectByIdForUpdate(7L)).thenReturn(RentalDeviceDO.builder().id(7L).build());
        when(lockMapper.selectActiveForUpdate(eq(7L), any())).thenReturn(List.of());

        RentalDeviceLockCreateReqVO req = new RentalDeviceLockCreateReqVO();
        req.setDeviceId(7L);
        req.setLockType("ORDER_HOLD");
        req.setReason("等待订单确认");
        req.setRentalOrderId(11L);
        req.setRentalOrderItemId(12L);
        req.setPlannedEndTime(LocalDateTime.now().plusHours(2));

        service.createManualLock(req);

        ArgumentCaptor<RentalDeviceLockDO> lockCaptor = ArgumentCaptor.forClass(RentalDeviceLockDO.class);
        verify(lockMapper).insert(lockCaptor.capture());
        assertEquals("ORDER_HOLD", lockCaptor.getValue().getLockType());
        assertEquals("MANUAL", lockCaptor.getValue().getSourceType());
        assertEquals("ACTIVE", lockCaptor.getValue().getStatus());
    }

    @Test
    void rejectsManualReleaseOfSystemManagedLock() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceLockMapper lockMapper = mock(RentalDeviceLockMapper.class);
        RentalDeviceLockService service = new RentalDeviceLockService(deviceMapper, lockMapper);
        when(lockMapper.selectByIdForUpdate(9L)).thenReturn(RentalDeviceLockDO.builder()
                .id(9L).deviceId(7L).lockType("MAINTENANCE").status("ACTIVE").build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.releaseManualLock(9L, 3L, "人工跳过"));

        assertEquals(RENTAL_DEVICE_LOCK_INVALID.getCode(), exception.getCode());
    }
}
