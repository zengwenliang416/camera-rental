package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for per-shop channel cursors.
 */
@Mapper
public interface XianyuSyncCursorMapper extends BaseMapperX<XianyuSyncCursorDO> {

    default XianyuSyncCursorDO selectByShopIdAndResourceType(Long shopId, String resourceType) {
        return selectOne(new LambdaQueryWrapperX<XianyuSyncCursorDO>()
                .eq(XianyuSyncCursorDO::getShopId, shopId)
                .eq(XianyuSyncCursorDO::getResourceType, resourceType));
    }

    default XianyuSyncCursorDO selectByShopIdAndResourceTypeForUpdate(Long shopId, String resourceType) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuSyncCursorDO>()
                .eq(XianyuSyncCursorDO::getShopId, shopId)
                .eq(XianyuSyncCursorDO::getResourceType, resourceType));
    }

}
