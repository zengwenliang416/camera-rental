package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceShipmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationDeviceMapper;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DELETE_BLOCKED;

@Service
public class RentalDeviceDeletionGuard {

    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final RentalDeviceLockService deviceLockService;
    private final RentalDeviceShipmentMapper shipmentMapper;
    private final RentalDeliveryDeviceRelMapper deliveryDeviceRelMapper;
    private final RentalReturnRegistrationDeviceMapper returnRegistrationDeviceMapper;

    public RentalDeviceDeletionGuard(RentalDeviceAssignmentMapper assignmentMapper,
                                     RentalScheduleMapper scheduleMapper,
                                     RentalDeviceLockService deviceLockService,
                                     RentalDeviceShipmentMapper shipmentMapper,
                                     RentalDeliveryDeviceRelMapper deliveryDeviceRelMapper,
                                     RentalReturnRegistrationDeviceMapper returnRegistrationDeviceMapper) {
        this.assignmentMapper = assignmentMapper;
        this.scheduleMapper = scheduleMapper;
        this.deviceLockService = deviceLockService;
        this.shipmentMapper = shipmentMapper;
        this.deliveryDeviceRelMapper = deliveryDeviceRelMapper;
        this.returnRegistrationDeviceMapper = returnRegistrationDeviceMapper;
    }

    public void validateDeletable(RentalDeviceDO device) {
        if (!"AVAILABLE".equals(device.getStatus())) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备状态不是 AVAILABLE");
        }
        if (device.getSourceType() != null
                || device.getSourceBizId() != null
                || device.getSourceItemId() != null) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备存在采购入库来源");
        }
        Long tenantId = device.getTenantId();
        Long deviceId = device.getId();
        if (assignmentMapper.countAllByDeviceId(tenantId, deviceId) > 0) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备存在分配历史");
        }
        if (scheduleMapper.countAllByDeviceId(tenantId, deviceId) > 0) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备存在排期历史");
        }
        if (!deviceLockService.getActiveLocksForUpdate(deviceId).isEmpty()) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备存在活动锁定");
        }
        if (shipmentMapper.countAllByDeviceId(tenantId, deviceId) > 0) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备存在发货记录");
        }
        if (deliveryDeviceRelMapper.countAllByDeviceId(tenantId, deviceId) > 0) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备存在物流关联");
        }
        if (returnRegistrationDeviceMapper.countAllByDeviceId(tenantId, deviceId) > 0) {
            throw exception(RENTAL_DEVICE_DELETE_BLOCKED, "设备存在客户退回登记");
        }
    }

}
