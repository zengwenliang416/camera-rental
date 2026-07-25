package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Rental order persistence keyed by its immutable source identity.
 */
@Mapper
public interface RentalOrderMapper extends BaseMapperX<RentalOrderDO> {

    default RentalOrderDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalOrderDO>()
                .eq(RentalOrderDO::getId, id));
    }

    default RentalOrderDO selectBySourceForUpdate(String sourceType, String sourceOrderId) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalOrderDO>()
                .eq(RentalOrderDO::getSourceType, sourceType)
                .eq(RentalOrderDO::getSourceOrderId, sourceOrderId));
    }

}
