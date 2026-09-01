package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductSkuMappingDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RentalChannelProductSkuMappingMapper
        extends BaseMapperX<RentalChannelProductSkuMappingDO> {

    default List<RentalChannelProductSkuMappingDO> selectListByProductRuleId(Long productRuleId) {
        return selectList(new LambdaQueryWrapper<RentalChannelProductSkuMappingDO>()
                .eq(RentalChannelProductSkuMappingDO::getProductRuleId, productRuleId)
                .orderByAsc(RentalChannelProductSkuMappingDO::getId));
    }

    default RentalChannelProductSkuMappingDO selectEnabledByRuleIdAndXgjSkuIdForUpdate(
            Long productRuleId, String xgjSkuId) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalChannelProductSkuMappingDO>()
                .eq(RentalChannelProductSkuMappingDO::getProductRuleId, productRuleId)
                .eq(RentalChannelProductSkuMappingDO::getXgjSkuId, xgjSkuId)
                .eq(RentalChannelProductSkuMappingDO::getEnabled, true));
    }

    default List<RentalChannelProductSkuMappingDO> selectListByProductRuleIds(Collection<Long> productRuleIds) {
        if (productRuleIds == null || productRuleIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<RentalChannelProductSkuMappingDO>()
                .in(RentalChannelProductSkuMappingDO::getProductRuleId, productRuleIds)
                .orderByAsc(RentalChannelProductSkuMappingDO::getId));
    }

    default int deleteByProductRuleId(Long productRuleId) {
        return deleteByProductRuleIdAndTenant(
                TenantContextHolder.getRequiredTenantId(), productRuleId);
    }

    @Delete("""
            DELETE FROM rental_channel_product_sku_mapping
            WHERE tenant_id = #{tenantId}
              AND product_rule_id = #{productRuleId}
            """)
    int deleteByProductRuleIdAndTenant(@Param("tenantId") Long tenantId,
                                       @Param("productRuleId") Long productRuleId);

}
