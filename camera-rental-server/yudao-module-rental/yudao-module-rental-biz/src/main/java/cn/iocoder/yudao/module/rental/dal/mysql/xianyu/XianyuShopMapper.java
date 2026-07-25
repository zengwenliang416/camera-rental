package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XianyuShopMapper extends BaseMapperX<XianyuShopDO> {

    default XianyuShopDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<XianyuShopDO>()
                .eq(XianyuShopDO::getTenantId, tenantId)
                .eq(XianyuShopDO::getId, id));
    }

    default XianyuShopDO selectByApplicationAndExternalShopId(Long applicationId, String externalShopId) {
        return selectOne(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getApplicationId, applicationId)
                .eq(XianyuShopDO::getExternalShopId, externalShopId));
    }

    /**
     * Channel identity for order sync is authorize_id (one seller_id may have many authorizations).
     */
    default XianyuShopDO selectByApplicationAndAuthorizeId(Long applicationId, String authorizeId) {
        return selectOne(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getApplicationId, applicationId)
                .eq(XianyuShopDO::getAuthorizeId, authorizeId));
    }

    default XianyuShopDO selectByAuthorizeId(String authorizeId) {
        return selectOne(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getAuthorizeId, authorizeId));
    }

    default List<XianyuShopDO> selectListByApplicationId(Long applicationId) {
        return selectList(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getApplicationId, applicationId));
    }

    default List<XianyuShopDO> selectValidListByExternalShopId(String externalShopId) {
        return selectList(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getExternalShopId, externalShopId)
                .eq(XianyuShopDO::getAuthorizationStatus, "VALID")
                .orderByAsc(XianyuShopDO::getId));
    }

}
