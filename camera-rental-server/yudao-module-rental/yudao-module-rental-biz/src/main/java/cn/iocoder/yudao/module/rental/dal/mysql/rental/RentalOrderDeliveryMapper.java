package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDeliveryDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalOrderDeliveryMapper extends BaseMapperX<RentalOrderDeliveryDO> {

    default RentalOrderDeliveryDO selectByRentalOrderId(Long rentalOrderId) {
        return selectOne(new LambdaQueryWrapper<RentalOrderDeliveryDO>()
                .eq(RentalOrderDeliveryDO::getRentalOrderId, rentalOrderId)
                .last("LIMIT 1"));
    }

}
