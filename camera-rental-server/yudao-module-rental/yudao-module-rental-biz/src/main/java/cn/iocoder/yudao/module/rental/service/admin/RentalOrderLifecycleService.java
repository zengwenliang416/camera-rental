package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_ORDER_CANCEL_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_ORDER_NOT_EXISTS;

/**
 * Cancels a rental order and releases its not-yet-dispatched allocations.
 * Dispatched devices must be returned to the warehouse first; returned history stays untouched.
 */
@Service
public class RentalOrderLifecycleService {

    static final String ORDER_STATUS_CANCELED = "CANCELED";
    static final String ASSIGNMENT_STATUS_CANCELED = "CANCELED";
    static final String ASSIGNMENT_STATUS_DISPATCHED = "DISPATCHED";
    static final String ASSIGNMENT_STATUS_DISPATCHED_PENDING_PLAN = "DISPATCHED_PENDING_PLAN";
    static final String SCHEDULE_STATUS_EFFECTIVE = "EFFECTIVE";
    static final String SCHEDULE_STATUS_CANCELED = "CANCELED";

    private final RentalOrderMapper orderMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final RentalDeviceMapper deviceMapper;

    public RentalOrderLifecycleService(RentalOrderMapper orderMapper,
                                       RentalDeviceAssignmentMapper assignmentMapper,
                                       RentalScheduleMapper scheduleMapper,
                                       RentalDeviceMapper deviceMapper) {
        this.orderMapper = orderMapper;
        this.assignmentMapper = assignmentMapper;
        this.scheduleMapper = scheduleMapper;
        this.deviceMapper = deviceMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        RentalOrderDO order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw exception(RENTAL_ORDER_NOT_EXISTS);
        }
        if (ORDER_STATUS_CANCELED.equals(order.getStatus())) {
            return;
        }
        List<RentalDeviceAssignmentDO> assignments =
                assignmentMapper.selectActiveListByRentalOrderId(orderId);
        for (RentalDeviceAssignmentDO assignment : assignments) {
            if (ASSIGNMENT_STATUS_DISPATCHED.equals(assignment.getStatus())
                    || ASSIGNMENT_STATUS_DISPATCHED_PENDING_PLAN.equals(assignment.getStatus())) {
                throw exception(RENTAL_ORDER_CANCEL_FAILED,
                        "订单存在已出库设备，请先完成回仓或换机后再取消");
            }
        }
        for (RentalDeviceAssignmentDO assignment : assignments) {
            cancelAssignment(assignment);
        }
        order.setStatus(ORDER_STATUS_CANCELED);
        order.setCancelReason(StringUtils.hasText(reason) ? reason.trim() : null);
        orderMapper.updateById(order);
    }

    private void cancelAssignment(RentalDeviceAssignmentDO assignment) {
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(assignment.getDeviceId());
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        if (assignment.getScheduleId() != null) {
            RentalScheduleDO schedule = scheduleMapper.selectByIdForUpdate(assignment.getScheduleId());
            if (schedule != null && SCHEDULE_STATUS_EFFECTIVE.equals(schedule.getStatus())) {
                schedule.setStatus(SCHEDULE_STATUS_CANCELED);
                scheduleMapper.updateById(schedule);
            }
        }
        assignment.setStatus(ASSIGNMENT_STATUS_CANCELED);
        assignmentMapper.updateById(assignment);
    }

}
