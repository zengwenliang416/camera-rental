package cn.iocoder.yudao.module.rental.service.admin;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DISPATCH_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_RETURN_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_UNASSIGN_FAILED;

/**
 * Warehouse-facing device lifecycle: dispatch (出库), return/inspect (回仓), unassign (撤销分配).
 * ERP quantity stock is not updated here — instance state is the rental authority.
 */
@Service
public class RentalDeviceOpsService {

    static final String DEVICE_AVAILABLE = "AVAILABLE";
    static final String DEVICE_RENTED = "RENTED";
    static final String DEVICE_MAINTENANCE = "MAINTENANCE";

    static final String ASSIGN_ASSIGNED = "ASSIGNED";
    static final String ASSIGN_DISPATCHED = "DISPATCHED";
    static final String ASSIGN_RETURNED = "RETURNED";
    static final String ASSIGN_CANCELED = "CANCELED";

    static final String SCHEDULE_EFFECTIVE = "EFFECTIVE";
    static final String SCHEDULE_CANCELED = "CANCELED";

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final RentalDeviceLockService lockService;
    private final Clock clock;

    @Autowired
    public RentalDeviceOpsService(RentalDeviceMapper deviceMapper,
                                  RentalDeviceAssignmentMapper assignmentMapper,
                                  RentalScheduleMapper scheduleMapper,
                                  RentalDeviceLockService lockService) {
        this(deviceMapper, assignmentMapper, scheduleMapper, lockService, Clock.system(BUSINESS_ZONE));
    }

    RentalDeviceOpsService(RentalDeviceMapper deviceMapper,
                           RentalDeviceAssignmentMapper assignmentMapper,
                           RentalScheduleMapper scheduleMapper,
                           RentalDeviceLockService lockService,
                           Clock clock) {
        this.deviceMapper = deviceMapper;
        this.assignmentMapper = assignmentMapper;
        this.scheduleMapper = scheduleMapper;
        this.lockService = lockService;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeviceOpsRespVO dispatch(RentalDeviceDispatchReqVO reqVO) {
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(reqVO.getDeviceId());
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(device.getEnabled())) {
            throw exception(RENTAL_DEVICE_DISPATCH_FAILED, "设备已停用");
        }
        if (DEVICE_RENTED.equals(device.getStatus())) {
            throw exception(RENTAL_DEVICE_DISPATCH_FAILED, "设备已在租出库");
        }
        if (!DEVICE_AVAILABLE.equals(device.getStatus())) {
            throw exception(RENTAL_DEVICE_DISPATCH_FAILED, "设备状态不可出库：" + device.getStatus());
        }

        RentalDeviceAssignmentDO assignment = resolveAssignmentForDispatch(reqVO, device.getId());
        if (!ASSIGN_ASSIGNED.equals(assignment.getStatus())) {
            throw exception(RENTAL_DEVICE_DISPATCH_FAILED, "分配状态不可出库：" + assignment.getStatus());
        }
        if (!device.getId().equals(assignment.getDeviceId())) {
            throw exception(RENTAL_DEVICE_DISPATCH_FAILED, "分配记录与设备不匹配");
        }

        device.setStatus(DEVICE_RENTED);
        deviceMapper.updateById(device);

        assignment.setStatus(ASSIGN_DISPATCHED);
        assignmentMapper.updateById(assignment);

        return toResp(device, assignment);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeviceOpsRespVO returnDevice(RentalDeviceReturnReqVO reqVO) {
        RentalDeviceDO device = resolveDeviceForReturn(reqVO);
        if (!DEVICE_RENTED.equals(device.getStatus())) {
            throw exception(RENTAL_DEVICE_RETURN_FAILED, "仅在租设备可回仓，当前：" + device.getStatus());
        }

        RentalDeviceAssignmentDO assignment = assignmentMapper.selectActiveByDeviceIdForUpdate(device.getId());
        if (assignment == null || !ASSIGN_DISPATCHED.equals(assignment.getStatus())) {
            throw exception(RENTAL_DEVICE_RETURN_FAILED, "未找到已出库的分配记录");
        }

        boolean passed = reqVO.getInspectPassed() == null || Boolean.TRUE.equals(reqVO.getInspectPassed());
        lockService.releaseSystemLockForLockedDevice(device.getId(),
                RentalDeviceLockTypeEnum.RETURN_INSPECTION, "INSPECTION_COMPLETED");
        if (passed) {
            lockService.releaseSystemLockForLockedDevice(device.getId(),
                    RentalDeviceLockTypeEnum.MAINTENANCE, "INSPECTION_PASSED");
        } else {
            lockService.createSystemLockForLockedDevice(device.getId(),
                    RentalDeviceLockTypeEnum.MAINTENANCE,
                    StringUtils.hasText(reqVO.getNote()) ? reqVO.getNote() : "INSPECTION_FAILED",
                    assignment.getRentalOrderId(), assignment.getRentalOrderItemId());
        }
        device.setStatus(passed ? DEVICE_AVAILABLE : DEVICE_MAINTENANCE);
        deviceMapper.updateById(device);

        narrowScheduleForReturn(assignment);
        LocalDateTime completedAt = LocalDateTime.now(clock);
        assignment.setStatus(ASSIGN_RETURNED)
                .setReturnedAt(completedAt)
                .setInspectionCompletedAt(completedAt)
                .setInspectionResult(passed ? "PASSED" : "FAILED")
                .setReturnNote(trimToNull(reqVO.getNote()));
        assignmentMapper.updateById(assignment);

        return toResp(device, assignment);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeviceOpsRespVO unassign(RentalDeviceUnassignReqVO reqVO) {
        RentalDeviceAssignmentDO snapshot = assignmentMapper.selectById(reqVO.getAssignmentId());
        if (snapshot == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(snapshot.getDeviceId());
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        RentalDeviceAssignmentDO assignment = assignmentMapper.selectByIdForUpdate(reqVO.getAssignmentId());
        if (assignment == null || !device.getId().equals(assignment.getDeviceId())) {
            throw exception(RENTAL_DEVICE_UNASSIGN_FAILED, "分配记录已变化，请刷新后重试");
        }
        if (ASSIGN_CANCELED.equals(assignment.getStatus())) {
            return toResp(device, assignment);
        }
        if (!ASSIGN_ASSIGNED.equals(assignment.getStatus())) {
            throw exception(RENTAL_DEVICE_UNASSIGN_FAILED,
                    "仅未出库的分配可撤销，当前：" + assignment.getStatus());
        }
        cancelAssignmentSchedule(assignment);
        assignment.setStatus(ASSIGN_CANCELED);
        assignmentMapper.updateById(assignment);
        return toResp(device, assignment);
    }

    /**
     * Occupancy ends at warehouse check-in: narrow the half-open end to tomorrow (Asia/Shanghai)
     * when the original plan extends further. Never extends or touches already-ended schedules.
     */
    private void narrowScheduleForReturn(RentalDeviceAssignmentDO assignment) {
        if (assignment.getScheduleId() == null) {
            return;
        }
        RentalScheduleDO schedule = scheduleMapper.selectByIdForUpdate(assignment.getScheduleId());
        if (schedule == null || !SCHEDULE_EFFECTIVE.equals(schedule.getStatus())) {
            return;
        }
        LocalDate newEndExclusive = LocalDate.now(clock).plusDays(1);
        if (newEndExclusive.isAfter(schedule.getOccupyStartDate())
                && newEndExclusive.isBefore(schedule.getOccupyEndDateExclusive())) {
            schedule.setOccupyEndDateExclusive(newEndExclusive);
            scheduleMapper.updateById(schedule);
        }
    }

    private void cancelAssignmentSchedule(RentalDeviceAssignmentDO assignment) {
        if (assignment.getScheduleId() == null) {
            return;
        }
        RentalScheduleDO schedule = scheduleMapper.selectByIdForUpdate(assignment.getScheduleId());
        if (schedule == null || !SCHEDULE_EFFECTIVE.equals(schedule.getStatus())) {
            return;
        }
        schedule.setStatus(SCHEDULE_CANCELED);
        scheduleMapper.updateById(schedule);
    }

    private RentalDeviceDO resolveDeviceForReturn(RentalDeviceReturnReqVO reqVO) {
        if (reqVO.getDeviceId() != null) {
            RentalDeviceDO device = deviceMapper.selectByIdForUpdate(reqVO.getDeviceId());
            if (device == null) {
                throw exception(RENTAL_DEVICE_NOT_EXISTS);
            }
            return device;
        }
        if (StringUtils.hasText(reqVO.getDeviceNo())) {
            RentalDeviceDO device = deviceMapper.selectByDeviceNoForUpdate(reqVO.getDeviceNo().trim());
            if (device == null) {
                throw exception(RENTAL_DEVICE_NOT_EXISTS);
            }
            return device;
        }
        throw exception(RENTAL_DEVICE_RETURN_FAILED, "缺少设备编号或设备 ID");
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private RentalDeviceAssignmentDO resolveAssignmentForDispatch(RentalDeviceDispatchReqVO reqVO, Long deviceId) {
        if (reqVO.getAssignmentId() != null) {
            RentalDeviceAssignmentDO byId = assignmentMapper.selectByIdForUpdate(reqVO.getAssignmentId());
            if (byId == null) {
                throw exception(RENTAL_DEVICE_DISPATCH_FAILED, "分配记录不存在");
            }
            return byId;
        }
        RentalDeviceAssignmentDO active = assignmentMapper.selectActiveByDeviceIdForUpdate(deviceId);
        if (active == null) {
            throw exception(RENTAL_DEVICE_DISPATCH_FAILED, "设备没有待出库的分配记录，请先分配订单");
        }
        return active;
    }

    private static RentalDeviceOpsRespVO toResp(RentalDeviceDO device, RentalDeviceAssignmentDO assignment) {
        RentalDeviceOpsRespVO vo = new RentalDeviceOpsRespVO();
        vo.setDeviceId(device.getId());
        vo.setDeviceNo(device.getDeviceNo());
        vo.setDeviceStatus(device.getStatus());
        vo.setAssignmentId(assignment.getId());
        vo.setAssignmentStatus(assignment.getStatus());
        return vo;
    }

}
