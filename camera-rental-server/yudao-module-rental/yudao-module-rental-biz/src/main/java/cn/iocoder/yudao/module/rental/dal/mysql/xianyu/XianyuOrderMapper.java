package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mapper for normalized channel orders.
 */
@Mapper
public interface XianyuOrderMapper extends BaseMapperX<XianyuOrderDO> {

    default PageResult<XianyuOrderDO> selectAdminPage(XianyuOrderPageReqVO pageReqVO) {
        LambdaQueryWrapperX<XianyuOrderDO> query = new LambdaQueryWrapperX<XianyuOrderDO>()
                .eqIfPresent(XianyuOrderDO::getShopId, pageReqVO.getShopId())
                .eqIfPresent(XianyuOrderDO::getConversionStatus, pageReqVO.getConversionStatus())
                .eqIfPresent(XianyuOrderDO::getExternalProductId, pageReqVO.getExternalProductId())
                .eqIfPresent(XianyuOrderDO::getExternalSkuId, pageReqVO.getExternalSkuId())
                .orderByDesc(XianyuOrderDO::getSourceUpdatedAt)
                .orderByDesc(XianyuOrderDO::getId);
        query.apply(pageReqVO.getStartDate() != null,
                "COALESCE(order_time, source_created_at, create_time) >= {0}",
                pageReqVO.getStartDate() != null ? pageReqVO.getStartDate().atStartOfDay() : null);
        query.apply(pageReqVO.getEndDate() != null,
                "COALESCE(order_time, source_created_at, create_time) < {0}",
                pageReqVO.getEndDate() != null ? pageReqVO.getEndDate().plusDays(1).atStartOfDay() : null);
        query.select(XianyuOrderDO.class, field -> !"detailJson".equals(field.getProperty())
                && !"goodsJson".equals(field.getProperty()));
        return selectPage(pageReqVO, query);
    }

    default Map<String, Object> selectRevenueSummary(Long shopId) {
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<XianyuOrderDO>()
                .select("COUNT(*) AS order_count",
                        "COALESCE(SUM(pay_amount), 0) AS rent_amount_fen",
                        "COALESCE(SUM(refund_amount), 0) AS refund_amount_fen")
                .eq(shopId != null, "shop_id", shopId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    default XianyuOrderDO selectByShopIdAndExternalOrderId(Long shopId, String externalOrderId) {
        return selectOne(new LambdaQueryWrapperX<XianyuOrderDO>()
                .eq(XianyuOrderDO::getShopId, shopId)
                .eq(XianyuOrderDO::getExternalOrderId, externalOrderId));
    }

    default List<XianyuOrderDO> selectListByShopIdsAndExternalOrderId(List<Long> shopIds, String externalOrderId) {
        return selectList(new LambdaQueryWrapperX<XianyuOrderDO>()
                .in(XianyuOrderDO::getShopId, shopIds)
                .eq(XianyuOrderDO::getExternalOrderId, externalOrderId)
                .orderByAsc(XianyuOrderDO::getShopId)
                .last("LIMIT 2"));
    }

    default List<XianyuOrderDO> selectRefreshStateList(Long shopId, List<String> externalOrderIds) {
        return selectList(new LambdaQueryWrapperX<XianyuOrderDO>()
                .select(XianyuOrderDO::getExternalOrderId, XianyuOrderDO::getSourceUpdatedAt,
                        XianyuOrderDO::getRawPayloadId)
                .eq(XianyuOrderDO::getShopId, shopId)
                .in(XianyuOrderDO::getExternalOrderId, externalOrderIds));
    }

    default List<XianyuOrderDO> selectMissingDetailRefs(int limit) {
        return selectList(missingDetailRefsQuery(limit));
    }

    static LambdaQueryWrapperX<XianyuOrderDO> missingDetailRefsQuery(int limit) {
        int boundedLimit = Math.max(1, Math.min(500, limit));
        LambdaQueryWrapperX<XianyuOrderDO> query = new LambdaQueryWrapperX<>();
        query.select(XianyuOrderDO::getId, XianyuOrderDO::getShopId,
                XianyuOrderDO::getExternalOrderId);
        query.isNull(XianyuOrderDO::getDetailJson);
        query.isNotNull(XianyuOrderDO::getExternalOrderId);
        query.ne(XianyuOrderDO::getExternalOrderId, "");
        query.orderByDesc(XianyuOrderDO::getSourceUpdatedAt);
        query.orderByDesc(XianyuOrderDO::getId);
        query.last("LIMIT " + boundedLimit);
        return query;
    }

    default XianyuOrderDO selectByShopIdAndExternalOrderIdForUpdate(Long shopId, String externalOrderId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuOrderDO>()
                .eq(XianyuOrderDO::getShopId, shopId)
                .eq(XianyuOrderDO::getExternalOrderId, externalOrderId));
    }

    default XianyuOrderDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuOrderDO>()
                .eq(XianyuOrderDO::getId, id));
    }

    default XianyuOrderDO selectNewestCursorCandidate(Long shopId, LocalDateTime windowStart,
                                                       LocalDateTime windowEnd) {
        return selectOne(new LambdaQueryWrapperX<XianyuOrderDO>()
                .eq(XianyuOrderDO::getShopId, shopId)
                .ge(XianyuOrderDO::getSourceUpdatedAt, windowStart)
                .le(XianyuOrderDO::getSourceUpdatedAt, windowEnd)
                .isNotNull(XianyuOrderDO::getSourceUpdatedAt)
                .orderByDesc(XianyuOrderDO::getSourceUpdatedAt)
                .orderByDesc(XianyuOrderDO::getExternalOrderId)
                .last("LIMIT 1"));
    }

}
