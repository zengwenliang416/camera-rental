package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceCategoryDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalDeviceCategoryMapper extends BaseMapperX<RentalDeviceCategoryDO> {

    default List<RentalDeviceCategoryDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapper<RentalDeviceCategoryDO>()
                .eq(RentalDeviceCategoryDO::getEnabled, true)
                .orderByAsc(RentalDeviceCategoryDO::getSortOrder)
                .orderByAsc(RentalDeviceCategoryDO::getId));
    }

    default RentalDeviceCategoryDO selectByCode(String categoryCode) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceCategoryDO>()
                .eq(RentalDeviceCategoryDO::getCategoryCode, categoryCode)
                .last("LIMIT 1"));
    }

}
