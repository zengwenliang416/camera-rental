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
        return selectCount(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                .eq(RentalDeviceAssignmentDO::getRentalOrderItemId, rentalOrderItemId)
                .eq(RentalDeviceAssignmentDO::getStatus, "ASSIGNED"));
    }

}
