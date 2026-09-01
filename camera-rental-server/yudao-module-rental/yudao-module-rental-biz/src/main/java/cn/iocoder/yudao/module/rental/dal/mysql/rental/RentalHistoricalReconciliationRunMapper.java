package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalHistoricalReconciliationRunDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalHistoricalReconciliationRunMapper
        extends BaseMapperX<RentalHistoricalReconciliationRunDO> {

    default RentalHistoricalReconciliationRunDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalHistoricalReconciliationRunDO>()
                .eq(RentalHistoricalReconciliationRunDO::getId, id));
    }

}

