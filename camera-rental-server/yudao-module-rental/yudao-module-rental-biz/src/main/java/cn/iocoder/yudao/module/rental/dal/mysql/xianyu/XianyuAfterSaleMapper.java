package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuAfterSaleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface XianyuAfterSaleMapper extends BaseMapperX<XianyuAfterSaleDO> {

    default PageResult<XianyuAfterSaleDO> selectAdminPage(PageParam pageParam, Long shopId, String status) {
        return selectPage(pageParam, new LambdaQueryWrapperX<XianyuAfterSaleDO>()
                .eqIfPresent(XianyuAfterSaleDO::getShopId, shopId)
                .eqIfPresent(XianyuAfterSaleDO::getAfterSaleStatus, status)
                .orderByDesc(XianyuAfterSaleDO::getSourceUpdatedAt)
                .orderByDesc(XianyuAfterSaleDO::getId));
    }

    default XianyuAfterSaleDO selectByShopIdAndExternalAfterSaleIdForUpdate(Long shopId, String externalAfterSaleId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuAfterSaleDO>()
                .eq(XianyuAfterSaleDO::getShopId, shopId)
                .eq(XianyuAfterSaleDO::getExternalAfterSaleId, externalAfterSaleId));
    }

    default XianyuAfterSaleDO selectNewestCursorCandidate(Long shopId, LocalDateTime windowStart,
                                                          LocalDateTime windowEnd) {
        return selectOne(new LambdaQueryWrapper<XianyuAfterSaleDO>()
                .eq(XianyuAfterSaleDO::getShopId, shopId)
                .ge(XianyuAfterSaleDO::getSourceUpdatedAt, windowStart)
                .le(XianyuAfterSaleDO::getSourceUpdatedAt, windowEnd)
                .isNotNull(XianyuAfterSaleDO::getSourceUpdatedAt)
                .orderByDesc(XianyuAfterSaleDO::getSourceUpdatedAt)
                .orderByDesc(XianyuAfterSaleDO::getExternalAfterSaleId)
                .last("LIMIT 1"));
    }

}
