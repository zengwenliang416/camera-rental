package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RentalOrderItemMapper extends BaseMapperX<RentalOrderItemDO> {

    default RentalOrderItemDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalOrderItemDO>()
                .eq(RentalOrderItemDO::getId, id));
    }

    default RentalOrderItemDO selectFirstByRentalOrderIdForUpdate(Long rentalOrderId) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalOrderItemDO>()
                .eq(RentalOrderItemDO::getRentalOrderId, rentalOrderId)
                .orderByAsc(RentalOrderItemDO::getId)
                .last("LIMIT 1"));
    }

    default List<RentalOrderItemDO> selectListByRentalOrderIds(Collection<Long> rentalOrderIds) {
        if (rentalOrderIds == null || rentalOrderIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<RentalOrderItemDO>()
                .in(RentalOrderItemDO::getRentalOrderId, rentalOrderIds)
                .orderByAsc(RentalOrderItemDO::getRentalOrderId)
                .orderByAsc(RentalOrderItemDO::getId));
    }

}
