package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalDeviceAssignmentMapper extends BaseMapperX<RentalDeviceAssignmentDO> {

    default RentalDeviceAssignmentDO selectByIdempotencyKeyForUpdate(String idempotencyKey) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                .eq(RentalDeviceAssignmentDO::getIdempotencyKey, idempotencyKey));
    }

    default long countAssignedByOrderItem(Long rentalOrderItemId) {
        // Active links still occupy the order item slot until returned.
        return selectCount(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                .eq(RentalDeviceAssignmentDO::getRentalOrderItemId, rentalOrderItemId)
                .in(RentalDeviceAssignmentDO::getStatus, "ASSIGNED", "DISPATCHED"));
    }

    default RentalDeviceAssignmentDO selectActiveByDeviceIdForUpdate(Long deviceId) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                .eq(RentalDeviceAssignmentDO::getDeviceId, deviceId)
                .in(RentalDeviceAssignmentDO::getStatus, "ASSIGNED", "DISPATCHED")
                .orderByDesc(RentalDeviceAssignmentDO::getId)
                .last("LIMIT 1"));
    }

    default RentalDeviceAssignmentDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                .eq(RentalDeviceAssignmentDO::getId, id));
    }

}
