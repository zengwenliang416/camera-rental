package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface XianyuProductSkuMapper extends BaseMapperX<XianyuProductSkuDO> {

    default XianyuProductSkuDO selectByProductIdAndExternalSkuIdForUpdate(Long productId, String externalSkuId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuProductSkuDO>()
                .eq(XianyuProductSkuDO::getProductId, productId)
                .eq(XianyuProductSkuDO::getExternalSkuId, externalSkuId));
    }

}
