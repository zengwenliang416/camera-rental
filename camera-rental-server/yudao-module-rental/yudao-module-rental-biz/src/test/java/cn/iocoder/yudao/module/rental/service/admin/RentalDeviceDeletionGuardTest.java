package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceShipmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationDeviceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DELETE_BLOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeviceDeletionGuardTest {

    private RentalDeviceAssignmentMapper assignmentMapper;
    private RentalScheduleMapper scheduleMapper;
    private RentalDeviceLockService deviceLockService;
    private RentalDeviceShipmentMapper shipmentMapper;
    private RentalDeliveryDeviceRelMapper deliveryDeviceRelMapper;
    private RentalReturnRegistrationDeviceMapper returnRegistrationDeviceMapper;
    private RentalDeviceDeletionGuard guard;

    @BeforeEach
    void setUp() {
        assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        scheduleMapper = mock(RentalScheduleMapper.class);
        deviceLockService = mock(RentalDeviceLockService.class);
        shipmentMapper = mock(RentalDeviceShipmentMapper.class);
        deliveryDeviceRelMapper = mock(RentalDeliveryDeviceRelMapper.class);
        returnRegistrationDeviceMapper = mock(RentalReturnRegistrationDeviceMapper.class);
        guard = new RentalDeviceDeletionGuard(assignmentMapper, scheduleMapper, deviceLockService, shipmentMapper,
                deliveryDeviceRelMapper, returnRegistrationDeviceMapper);
    }

    @Test
    void allowsAvailableDeviceWithoutSourceOrReferences() {
        guard.validateDeletable(availableDevice());
    }

    @Test
    void rejectsNonAvailableDevice() {
        RentalDeviceDO device = availableDevice();
        device.setStatus("RENTED");
        assertBlocked(device);
    }

    @Test
    void rejectsPurchaseSourcedDevice() {
        RentalDeviceDO device = availableDevice();
        device.setSourceType("ERP_PURCHASE_IN");
        assertBlocked(device);
    }

    @Test
    void rejectsAssignmentHistory() {
        when(assignmentMapper.countAllByDeviceId(1L, 8L)).thenReturn(1L);
        assertBlocked(availableDevice());
    }

    @Test
    void rejectsScheduleHistory() {
        when(scheduleMapper.countAllByDeviceId(1L, 8L)).thenReturn(1L);
        assertBlocked(availableDevice());
    }

    @Test
    void rejectsActiveOrderHold() {
        when(deviceLockService.getActiveLocksForUpdate(8L)).thenReturn(List.of(
                RentalDeviceLockDO.builder().lockType("ORDER_HOLD").status("ACTIVE").build()));
        assertBlocked(availableDevice());
    }

    @Test
    void rejectsActiveManualHold() {
        when(deviceLockService.getActiveLocksForUpdate(8L)).thenReturn(List.of(
                RentalDeviceLockDO.builder().lockType("MANUAL_HOLD").status("ACTIVE").build()));
        assertBlocked(availableDevice());
    }

    @Test
    void allowsReleasedOrExpiredLockHistory() {
        guard.validateDeletable(availableDevice());
        verify(deviceLockService).getActiveLocksForUpdate(8L);
    }

    @Test
    void rejectsShipmentHistory() {
        when(shipmentMapper.countAllByDeviceId(1L, 8L)).thenReturn(1L);
        assertBlocked(availableDevice());
    }

    @Test
    void rejectsDeliveryHistory() {
        when(deliveryDeviceRelMapper.countAllByDeviceId(1L, 8L)).thenReturn(1L);
        assertBlocked(availableDevice());
    }

    @Test
    void rejectsReturnRegistrationHistory() {
        when(returnRegistrationDeviceMapper.countAllByDeviceId(1L, 8L)).thenReturn(1L);
        assertBlocked(availableDevice());
    }

    private void assertBlocked(RentalDeviceDO device) {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> guard.validateDeletable(device));
        assertEquals(RENTAL_DEVICE_DELETE_BLOCKED.getCode(), ex.getCode());
    }

    private static RentalDeviceDO availableDevice() {
        RentalDeviceDO device = RentalDeviceDO.builder().id(8L).status("AVAILABLE").build();
        device.setTenantId(1L);
        return device;
    }

}
