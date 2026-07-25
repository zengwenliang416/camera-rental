package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductMappingDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Locks a product mapping while a channel order is converted.
 */
@Mapper
public interface XianyuProductMappingMapper extends BaseMapperX<XianyuProductMappingDO> {

    default XianyuProductMappingDO selectByShopProductSkuForUpdate(Long shopId, String externalProductId,
                                                                     String externalSkuId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuProductMappingDO>()
                .eq(XianyuProductMappingDO::getShopId, shopId)
                .eq(XianyuProductMappingDO::getExternalProductId, externalProductId)
                .eq(XianyuProductMappingDO::getExternalSkuId, externalSkuId));
    }

}
