package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface XianyuProductMapper extends BaseMapperX<XianyuProductDO> {

    default XianyuProductDO selectByShopIdAndXgjProductIdForUpdate(Long shopId, String xgjProductId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .eq(XianyuProductDO::getXgjProductId, xgjProductId));
    }

    default XianyuProductDO selectByShopIdAndXgjProductId(Long shopId, String xgjProductId) {
        return selectOne(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .eq(XianyuProductDO::getXgjProductId, xgjProductId));
    }

    default XianyuProductDO selectByShopIdAndXianyuItemId(Long shopId, String xianyuItemId) {
        return selectOne(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .eq(XianyuProductDO::getXianyuItemId, xianyuItemId));
    }

    default List<XianyuProductDO> selectRefreshStateList(Long shopId, List<String> xgjProductIds) {
        if (xgjProductIds == null || xgjProductIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .in(XianyuProductDO::getXgjProductId, xgjProductIds));
    }

    default List<XianyuProductDO> selectListByXgjProductId(String xgjProductId) {
        return selectList(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getXgjProductId, xgjProductId));
    }

    default XianyuProductDO selectNewestCursorCandidate(Long shopId, LocalDateTime windowStart,
                                                        LocalDateTime windowEnd) {
        return selectOne(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .ge(XianyuProductDO::getSourceUpdatedAt, windowStart)
                .le(XianyuProductDO::getSourceUpdatedAt, windowEnd)
                .isNotNull(XianyuProductDO::getSourceUpdatedAt)
                .orderByDesc(XianyuProductDO::getSourceUpdatedAt)
                .orderByDesc(XianyuProductDO::getXgjProductId)
                .last("LIMIT 1"));
    }

}
