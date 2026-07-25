package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface XianyuProductMapper extends BaseMapperX<XianyuProductDO> {

    default XianyuProductDO selectByShopIdAndExternalProductIdForUpdate(Long shopId, String externalProductId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .eq(XianyuProductDO::getExternalProductId, externalProductId));
    }

    default XianyuProductDO selectByShopIdAndExternalProductId(Long shopId, String externalProductId) {
        return selectOne(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .eq(XianyuProductDO::getExternalProductId, externalProductId));
    }

    default List<XianyuProductDO> selectRefreshStateList(Long shopId, List<String> externalProductIds) {
        if (externalProductIds == null || externalProductIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .in(XianyuProductDO::getExternalProductId, externalProductIds));
    }

    default List<XianyuProductDO> selectListByExternalProductId(String externalProductId) {
        return selectList(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getExternalProductId, externalProductId));
    }

    default XianyuProductDO selectNewestCursorCandidate(Long shopId, LocalDateTime windowStart,
                                                        LocalDateTime windowEnd) {
        return selectOne(new LambdaQueryWrapper<XianyuProductDO>()
                .eq(XianyuProductDO::getShopId, shopId)
                .ge(XianyuProductDO::getSourceUpdatedAt, windowStart)
                .le(XianyuProductDO::getSourceUpdatedAt, windowEnd)
                .isNotNull(XianyuProductDO::getSourceUpdatedAt)
                .orderByDesc(XianyuProductDO::getSourceUpdatedAt)
                .orderByDesc(XianyuProductDO::getExternalProductId)
                .last("LIMIT 1"));
    }

}
