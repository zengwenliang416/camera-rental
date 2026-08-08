package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLockCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLockRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceLockMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.enums.rental.RentalDeviceLockStatusEnum;
import cn.iocoder.yudao.module.rental.enums.rental.RentalDeviceLockTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_LOCK_CONFLICT;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_LOCK_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_LOCK_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;

@Service
public class RentalDeviceLockService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_SYSTEM = "SYSTEM";

    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceLockMapper lockMapper;

    public RentalDeviceLockService(RentalDeviceMapper deviceMapper, RentalDeviceLockMapper lockMapper) {
        this.deviceMapper = deviceMapper;
        this.lockMapper = lockMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeviceLockRespVO createManualLock(RentalDeviceLockCreateReqVO reqVO) {
        RentalDeviceLockTypeEnum lockType = parseType(reqVO.getLockType());
        if (!lockType.isManuallyManaged()) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "系统管理的锁定类型不能人工创建");
        }
        LocalDateTime now = now();
        if (lockType == RentalDeviceLockTypeEnum.ORDER_HOLD && reqVO.getPlannedEndTime() == null) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "订单预留必须设置计划结束时间");
        }
        if (reqVO.getPlannedEndTime() != null && !reqVO.getPlannedEndTime().isAfter(now)) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "计划结束时间必须晚于当前时间");
        }
        if (lockType == RentalDeviceLockTypeEnum.ORDER_HOLD
                && (reqVO.getRentalOrderId() == null || reqVO.getRentalOrderItemId() == null)) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "订单预留必须关联订单和订单明细");
        }
        lockDevice(reqVO.getDeviceId());
        expireElapsed(reqVO.getDeviceId(), now);
        rejectWhenActive(reqVO.getDeviceId(), now);
        RentalDeviceLockDO lock = buildLock(reqVO.getDeviceId(), lockType, reqVO.getReason(),
                reqVO.getRentalOrderId(), reqVO.getRentalOrderItemId(), SOURCE_MANUAL, now,
                reqVO.getPlannedEndTime());
        lockMapper.insert(lock);
        return toResp(lock);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeviceLockRespVO releaseManualLock(Long id, Long userId, String reason) {
        RentalDeviceLockDO lock = lockMapper.selectByIdForUpdate(id);
        if (lock == null) {
            throw exception(RENTAL_DEVICE_LOCK_NOT_EXISTS);
        }
        RentalDeviceLockTypeEnum lockType = parseType(lock.getLockType());
        if (!lockType.isManuallyManaged()) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "系统管理的锁定必须由所属生命周期解除");
        }
        if (!RentalDeviceLockStatusEnum.ACTIVE.name().equals(lock.getStatus())) {
            return toResp(lock);
        }
        release(lock, userId, reason, now());
        lockMapper.updateById(lock);
        return toResp(lock);
    }

    public List<RentalDeviceLockDO> getActiveLocksForUpdate(Long deviceId) {
        LocalDateTime now = now();
        expireElapsed(deviceId, now);
        return lockMapper.selectActiveForUpdate(deviceId, now);
    }

    public RentalDeviceLockDO createSystemLockForLockedDevice(Long deviceId, RentalDeviceLockTypeEnum lockType,
                                                               String reason, Long rentalOrderId,
                                                               Long rentalOrderItemId) {
        if (lockType.isManuallyManaged()) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "人工锁定类型不能由系统生命周期创建");
        }
        LocalDateTime now = now();
        expireElapsed(deviceId, now);
        RentalDeviceLockDO existing = lockMapper.selectActiveByTypeForUpdate(deviceId, lockType.name(), now);
        if (existing != null) {
            return existing;
        }
        RentalDeviceLockDO lock = buildLock(deviceId, lockType, reason, rentalOrderId, rentalOrderItemId,
                SOURCE_SYSTEM, now, null);
        lockMapper.insert(lock);
        return lock;
    }

    public void releaseSystemLockForLockedDevice(Long deviceId, RentalDeviceLockTypeEnum lockType, String reason) {
        if (lockType.isManuallyManaged()) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "人工锁定类型不能由系统生命周期解除");
        }
        LocalDateTime now = now();
        expireElapsed(deviceId, now);
        RentalDeviceLockDO existing = lockMapper.selectActiveByTypeForUpdate(deviceId, lockType.name(), now);
        if (existing == null) {
            return;
        }
        release(existing, null, reason, now);
        lockMapper.updateById(existing);
    }

    private RentalDeviceDO lockDevice(Long deviceId) {
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(deviceId);
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        return device;
    }

    private void rejectWhenActive(Long deviceId, LocalDateTime now) {
        List<RentalDeviceLockDO> activeLocks = lockMapper.selectActiveForUpdate(deviceId, now);
        if (!activeLocks.isEmpty()) {
            throw exception(RENTAL_DEVICE_LOCK_CONFLICT, activeLocks.get(0).getLockType());
        }
    }

    private void expireElapsed(Long deviceId, LocalDateTime now) {
        lockMapper.expireElapsed(deviceId, now);
    }

    private static RentalDeviceLockDO buildLock(Long deviceId, RentalDeviceLockTypeEnum lockType, String reason,
                                                Long rentalOrderId, Long rentalOrderItemId, String sourceType,
                                                LocalDateTime startTime, LocalDateTime plannedEndTime) {
        return RentalDeviceLockDO.builder()
                .deviceId(deviceId)
                .lockType(lockType.name())
                .reason(reason)
                .rentalOrderId(rentalOrderId)
                .rentalOrderItemId(rentalOrderItemId)
                .sourceType(sourceType)
                .startTime(startTime)
                .plannedEndTime(plannedEndTime)
                .status(RentalDeviceLockStatusEnum.ACTIVE.name())
                .build();
    }

    private static void release(RentalDeviceLockDO lock, Long userId, String reason, LocalDateTime now) {
        lock.setStatus(RentalDeviceLockStatusEnum.RELEASED.name());
        lock.setReleasedAt(now);
        lock.setReleasedBy(userId);
        lock.setReleaseReason(reason);
    }

    private static RentalDeviceLockTypeEnum parseType(String lockType) {
        try {
            return RentalDeviceLockTypeEnum.valueOf(lockType);
        } catch (RuntimeException ex) {
            throw exception(RENTAL_DEVICE_LOCK_INVALID, "未知锁定类型");
        }
    }

    private static RentalDeviceLockRespVO toResp(RentalDeviceLockDO lock) {
        RentalDeviceLockRespVO vo = new RentalDeviceLockRespVO();
        vo.setId(lock.getId());
        vo.setDeviceId(lock.getDeviceId());
        vo.setLockType(lock.getLockType());
        vo.setReason(lock.getReason());
        vo.setRentalOrderId(lock.getRentalOrderId());
        vo.setRentalOrderItemId(lock.getRentalOrderItemId());
        vo.setSourceType(lock.getSourceType());
        vo.setStartTime(lock.getStartTime());
        vo.setPlannedEndTime(lock.getPlannedEndTime());
        vo.setReleasedAt(lock.getReleasedAt());
        vo.setReleasedBy(lock.getReleasedBy());
        vo.setReleaseReason(lock.getReleaseReason());
        vo.setStatus(lock.getStatus());
        return vo;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
