package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RentalDeviceLockMapper extends BaseMapperX<RentalDeviceLockDO> {

    default RentalDeviceLockDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceLockDO>()
                .eq(RentalDeviceLockDO::getId, id));
    }

    default List<RentalDeviceLockDO> selectActiveForUpdate(Long deviceId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapper<RentalDeviceLockDO>()
                .eq(RentalDeviceLockDO::getDeviceId, deviceId)
                .eq(RentalDeviceLockDO::getStatus, "ACTIVE")
                .and(wrapper -> wrapper.isNull(RentalDeviceLockDO::getPlannedEndTime)
                        .or().gt(RentalDeviceLockDO::getPlannedEndTime, now))
                .last("FOR UPDATE"));
    }

    default RentalDeviceLockDO selectActiveByTypeForUpdate(Long deviceId, String lockType, LocalDateTime now) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceLockDO>()
                .eq(RentalDeviceLockDO::getDeviceId, deviceId)
                .eq(RentalDeviceLockDO::getLockType, lockType)
                .eq(RentalDeviceLockDO::getStatus, "ACTIVE")
                .and(wrapper -> wrapper.isNull(RentalDeviceLockDO::getPlannedEndTime)
                        .or().gt(RentalDeviceLockDO::getPlannedEndTime, now))
                .last("LIMIT 1"));
    }

    default int expireElapsed(Long deviceId, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<RentalDeviceLockDO>()
                .eq(RentalDeviceLockDO::getDeviceId, deviceId)
                .eq(RentalDeviceLockDO::getStatus, "ACTIVE")
                .isNotNull(RentalDeviceLockDO::getPlannedEndTime)
                .le(RentalDeviceLockDO::getPlannedEndTime, now)
                .set(RentalDeviceLockDO::getStatus, "EXPIRED")
                .set(RentalDeviceLockDO::getReleasedAt, now)
                .set(RentalDeviceLockDO::getReleaseReason, "PLANNED_END_REACHED"));
    }
}
