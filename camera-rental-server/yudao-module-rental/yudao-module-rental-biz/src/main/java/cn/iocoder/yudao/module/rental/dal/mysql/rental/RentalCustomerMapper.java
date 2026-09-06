package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalCustomerDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalCustomerMapper extends BaseMapperX<RentalCustomerDO> {

    default RentalCustomerDO selectByMobile(String mobile) {
        return selectOne(new LambdaQueryWrapper<RentalCustomerDO>()
                .eq(RentalCustomerDO::getMobile, mobile)
                .last("LIMIT 1"));
    }

}
