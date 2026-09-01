package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XianyuProductSkuMapper extends BaseMapperX<XianyuProductSkuDO> {

    default XianyuProductSkuDO selectByProductIdAndXgjSkuIdForUpdate(Long productId, String xgjSkuId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuProductSkuDO>()
                .eq(XianyuProductSkuDO::getProductId, productId)
                .eq(XianyuProductSkuDO::getXgjSkuId, xgjSkuId));
    }

    default List<XianyuProductSkuDO> selectListByProductId(Long productId) {
        return selectList(new LambdaQueryWrapper<XianyuProductSkuDO>()
                .eq(XianyuProductSkuDO::getProductId, productId)
                .orderByAsc(XianyuProductSkuDO::getId));
    }

}
