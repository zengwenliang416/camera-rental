package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceReturnReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DISPATCH_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_RETURN_FAILED;

/**
 * Warehouse-facing device lifecycle: dispatch (出库) and return/inspect (回仓).
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

    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;

    public RentalDeviceOpsService(RentalDeviceMapper deviceMapper,
                                  RentalDeviceAssignmentMapper assignmentMapper) {
        this.deviceMapper = deviceMapper;
        this.assignmentMapper = assignmentMapper;
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
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(reqVO.getDeviceId());
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        if (!DEVICE_RENTED.equals(device.getStatus())) {
            throw exception(RENTAL_DEVICE_RETURN_FAILED, "仅在租设备可回仓，当前：" + device.getStatus());
        }

        RentalDeviceAssignmentDO assignment = assignmentMapper.selectActiveByDeviceIdForUpdate(device.getId());
        if (assignment == null || !ASSIGN_DISPATCHED.equals(assignment.getStatus())) {
            throw exception(RENTAL_DEVICE_RETURN_FAILED, "未找到已出库的分配记录");
        }

        boolean passed = reqVO.getInspectPassed() == null || Boolean.TRUE.equals(reqVO.getInspectPassed());
        device.setStatus(passed ? DEVICE_AVAILABLE : DEVICE_MAINTENANCE);
        deviceMapper.updateById(device);

        assignment.setStatus(ASSIGN_RETURNED);
        // Note is not persisted in V1 schema; keep API for staff remark forward-compat.
        if (StringUtils.hasText(reqVO.getNote())) {
            // no-op column yet
        }
        assignmentMapper.updateById(assignment);

        return toResp(device, assignment);
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
