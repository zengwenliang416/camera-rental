package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelReconciliationRunDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalChannelReconciliationRunMapper
        extends BaseMapperX<RentalChannelReconciliationRunDO> {

    List<String> ACTIVE_STATUSES = List.of("PENDING", "RUNNING");

    default RentalChannelReconciliationRunDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<RentalChannelReconciliationRunDO>()
                .eq(RentalChannelReconciliationRunDO::getTenantId, tenantId)
                .eq(RentalChannelReconciliationRunDO::getId, id));
    }

    default boolean existsActiveByTenantIdAndProductRuleId(Long tenantId, Long productRuleId) {
        return selectCount(new LambdaQueryWrapper<RentalChannelReconciliationRunDO>()
                .eq(RentalChannelReconciliationRunDO::getTenantId, tenantId)
                .eq(RentalChannelReconciliationRunDO::getProductRuleId, productRuleId)
                .in(RentalChannelReconciliationRunDO::getStatus, ACTIVE_STATUSES)) > 0;
    }

}
