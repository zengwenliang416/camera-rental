package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRulePageReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductRuleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelRuleImpactDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RentalChannelProductRuleMapper extends BaseMapperX<RentalChannelProductRuleDO> {

    default RentalChannelProductRuleDO selectByShopIdAndItemId(Long shopId, String xianyuItemId) {
        return selectOne(new LambdaQueryWrapper<RentalChannelProductRuleDO>()
                .eq(RentalChannelProductRuleDO::getShopId, shopId)
                .eq(RentalChannelProductRuleDO::getXianyuItemId, xianyuItemId));
    }

    default RentalChannelProductRuleDO selectEnabledByShopIdAndItemIdForUpdate(
            Long shopId, String xianyuItemId) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalChannelProductRuleDO>()
                .eq(RentalChannelProductRuleDO::getShopId, shopId)
                .eq(RentalChannelProductRuleDO::getXianyuItemId, xianyuItemId)
                .eq(RentalChannelProductRuleDO::getEnabled, true));
    }

    default List<RentalChannelProductRuleDO> selectEnabledListByXianyuItemIds(
            Collection<String> xianyuItemIds) {
        if (xianyuItemIds == null || xianyuItemIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<RentalChannelProductRuleDO>()
                .in(RentalChannelProductRuleDO::getXianyuItemId, xianyuItemIds)
                .eq(RentalChannelProductRuleDO::getEnabled, true));
    }

    default PageResult<RentalChannelProductRuleDO> selectPage(RentalChannelProductRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RentalChannelProductRuleDO>()
                .eqIfPresent(RentalChannelProductRuleDO::getShopId, reqVO.getShopId())
                .eqIfPresent(RentalChannelProductRuleDO::getHandlingPolicy, reqVO.getHandlingPolicy())
                .eqIfPresent(RentalChannelProductRuleDO::getEnabled, reqVO.getEnabled())
                .and(reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank(),
                        query -> query.like(RentalChannelProductRuleDO::getXianyuItemId, reqVO.getKeyword().trim())
                                .or()
                                .like(RentalChannelProductRuleDO::getProductTitleSnapshot,
                                        reqVO.getKeyword().trim()))
                .orderByDesc(RentalChannelProductRuleDO::getId));
    }

    default int updateByIdAndVersion(RentalChannelProductRuleDO rule, Long tenantId, Integer expectedVersion) {
        return update(rule, new LambdaUpdateWrapper<RentalChannelProductRuleDO>()
                .eq(RentalChannelProductRuleDO::getId, rule.getId())
                .eq(RentalChannelProductRuleDO::getTenantId, tenantId)
                .eq(RentalChannelProductRuleDO::getLockVersion, expectedVersion));
    }

    default RentalChannelRuleImpactDO selectImpact(Long shopId, String xianyuItemId) {
        return selectImpactByTenant(TenantContextHolder.getRequiredTenantId(), shopId, xianyuItemId);
    }

    @Select("""
            SELECT COUNT(*) AS scanned_count,
                   SUM(CASE WHEN ro.id IS NULL
                             AND xo.conversion_status <> 'REVIEW_REQUIRED'
                            THEN 1 ELSE 0 END) AS without_internal_order_count,
                   SUM(CASE WHEN ro.id IS NOT NULL
                             AND NOT EXISTS (
                               SELECT 1 FROM rental_device_assignment a
                               WHERE a.tenant_id = xo.tenant_id
                                 AND a.rental_order_id = ro.id
                                 AND a.deleted = b'0'
                             )
                             AND xo.conversion_status <> 'REVIEW_REQUIRED'
                            THEN 1 ELSE 0 END) AS mutable_internal_order_count,
                   SUM(CASE WHEN ro.id IS NOT NULL
                             AND EXISTS (
                               SELECT 1 FROM rental_device_assignment a
                               WHERE a.tenant_id = xo.tenant_id
                                 AND a.rental_order_id = ro.id
                                 AND a.deleted = b'0'
                            )
                            THEN 1 ELSE 0 END) AS protected_order_count,
                   SUM(CASE WHEN xo.conversion_status = 'REVIEW_REQUIRED'
                             AND NOT EXISTS (
                               SELECT 1 FROM rental_device_assignment a
                               WHERE a.tenant_id = xo.tenant_id
                                 AND a.rental_order_id = ro.id
                                 AND a.deleted = b'0'
                             )
                            THEN 1 ELSE 0 END) AS review_required_count
            FROM xianyu_order xo
            LEFT JOIN rental_order ro
              ON ro.tenant_id = xo.tenant_id
             AND ro.channel_order_id = xo.id
             AND ro.deleted = b'0'
            WHERE xo.tenant_id = #{tenantId}
              AND xo.shop_id = #{shopId}
              AND xo.xianyu_item_id = #{xianyuItemId}
              AND xo.deleted = b'0'
            """)
    RentalChannelRuleImpactDO selectImpactByTenant(@Param("tenantId") Long tenantId,
                                                   @Param("shopId") Long shopId,
                                                   @Param("xianyuItemId") String xianyuItemId);

}
