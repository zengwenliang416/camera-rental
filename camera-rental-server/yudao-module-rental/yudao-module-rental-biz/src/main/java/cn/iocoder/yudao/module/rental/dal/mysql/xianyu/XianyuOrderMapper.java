package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Mapper for normalized channel orders.
 */
@Mapper
public interface XianyuOrderMapper extends BaseMapperX<XianyuOrderDO> {

    default PageResult<XianyuOrderDO> selectAdminPage(XianyuOrderPageReqVO pageReqVO) {
        return selectPage(pageReqVO, adminPageQuery(pageReqVO));
    }

    static LambdaQueryWrapper<XianyuOrderDO> adminPageQuery(XianyuOrderPageReqVO pageReqVO) {
        LambdaQueryWrapper<XianyuOrderDO> query = new LambdaQueryWrapperX<XianyuOrderDO>()
                .eqIfPresent(XianyuOrderDO::getShopId, pageReqVO.getShopId())
                .eqIfPresent(XianyuOrderDO::getOrderStatus, pageReqVO.getOrderStatus())
                .eqIfPresent(XianyuOrderDO::getConversionStatus, pageReqVO.getConversionStatus())
                .likeIfPresent(XianyuOrderDO::getExternalOrderId, pageReqVO.getExternalOrderId())
                .eqIfPresent(XianyuOrderDO::getXgjProductId, pageReqVO.getXgjProductId())
                .eqIfPresent(XianyuOrderDO::getXianyuItemId, pageReqVO.getXianyuItemId())
                .eqIfPresent(XianyuOrderDO::getXgjSkuId, pageReqVO.getXgjSkuId())
                .eqIfPresent(XianyuOrderDO::getXianyuSkuId, pageReqVO.getXianyuSkuId())
                .eqIfPresent(XianyuOrderDO::getShipDate, pageReqVO.getShipDate())
                .orderByDesc(XianyuOrderDO::getSourceUpdatedAt)
                .orderByDesc(XianyuOrderDO::getId);
        query.apply(Boolean.TRUE.equals(pageReqVO.getUnremarked()),
                "(seller_remark IS NULL OR TRIM(seller_remark) = '')");
        query.apply(pageReqVO.getStartDate() != null,
                "COALESCE(order_time, source_created_at, create_time) >= {0}",
                pageReqVO.getStartDate() != null ? pageReqVO.getStartDate().atStartOfDay() : null);
        query.apply(pageReqVO.getEndDate() != null,
                "COALESCE(order_time, source_created_at, create_time) < {0}",
                pageReqVO.getEndDate() != null ? pageReqVO.getEndDate().plusDays(1).atStartOfDay() : null);
        query.apply(pageReqVO.getRentalStartDate() != null && pageReqVO.getRentalEndDate() != null,
                """
                (
                    EXISTS (
                        SELECT 1
                          FROM rental_order ro
                         WHERE ro.id = xianyu_order.rental_order_id
                           AND ro.tenant_id = xianyu_order.tenant_id
                           AND ro.deleted = b'0'
                           AND ro.billable_start_date IS NOT NULL
                           AND ro.billable_end_date IS NOT NULL
                           AND ro.billable_start_date <= {0}
                           AND ro.billable_end_date >= {1}
                    )
                    OR (
                        NOT EXISTS (
                            SELECT 1
                              FROM rental_order ro_period
                             WHERE ro_period.id = xianyu_order.rental_order_id
                               AND ro_period.tenant_id = xianyu_order.tenant_id
                               AND ro_period.deleted = b'0'
                               AND ro_period.billable_start_date IS NOT NULL
                               AND ro_period.billable_end_date IS NOT NULL
                        )
                        AND billable_start_date IS NOT NULL
                        AND billable_end_date IS NOT NULL
                        AND billable_start_date <= {0}
                        AND billable_end_date >= {1}
                    )
                )
                """,
                pageReqVO.getRentalEndDate(), pageReqVO.getRentalStartDate());
        // Receiver fields are persisted separately; list reads do not need raw payload blobs.
        query.select(XianyuOrderDO.class, field -> !"detailJson".equals(field.getProperty())
                && !"goodsJson".equals(field.getProperty())
                && !"payNo".equals(field.getProperty()));
        return query;
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

    default List<XianyuOrderDO> selectListByExternalOrderId(String externalOrderId) {
        return selectList(new LambdaQueryWrapperX<XianyuOrderDO>()
                .select(XianyuOrderDO::getId, XianyuOrderDO::getTenantId,
                        XianyuOrderDO::getExternalOrderId, XianyuOrderDO::getReceiverMobile,
                        XianyuOrderDO::getRentalOrderId)
                .eq(XianyuOrderDO::getExternalOrderId, externalOrderId)
                .orderByAsc(XianyuOrderDO::getId)
                .last("LIMIT 2"));
    }

    default List<XianyuOrderDO> selectListByReceiverMobileLast4(String mobileLast4) {
        return selectList(receiverMobileLast4Query(mobileLast4));
    }

    @Select("""
            SELECT xo.id, xo.tenant_id, xo.external_order_id, xo.receiver_mobile,
                   xo.rental_order_id, xo.source_updated_at
              FROM xianyu_order xo
              JOIN rental_order ro
                ON ro.id = xo.rental_order_id
               AND ro.tenant_id = xo.tenant_id
               AND ro.deleted = b'0'
              JOIN rental_device_assignment assignment
                ON assignment.rental_order_id = ro.id
               AND assignment.tenant_id = ro.tenant_id
               AND assignment.deleted = b'0'
               AND assignment.status IN ('ASSIGNED', 'DISPATCHED', 'DISPATCHED_PENDING_PLAN')
              JOIN rental_device device
                ON device.id = assignment.device_id
               AND device.tenant_id = assignment.tenant_id
               AND device.deleted = b'0'
             WHERE xo.deleted = b'0'
               AND (UPPER(device.device_no) = #{machineCode}
                    OR UPPER(COALESCE(device.legacy_device_no, '')) = #{machineCode}
                    OR UPPER(COALESCE(device.serial_number, '')) = #{machineCode})
             ORDER BY xo.source_updated_at DESC, xo.id DESC
             LIMIT 3
            """)
    List<XianyuOrderDO> selectListByAssignedMachineCode(@Param("machineCode") String machineCode);

    static LambdaQueryWrapperX<XianyuOrderDO> receiverMobileLast4Query(String mobileLast4) {
        LambdaQueryWrapperX<XianyuOrderDO> query = new LambdaQueryWrapperX<>();
        query.select(XianyuOrderDO::getId, XianyuOrderDO::getTenantId,
                XianyuOrderDO::getExternalOrderId, XianyuOrderDO::getReceiverMobile,
                XianyuOrderDO::getRentalOrderId, XianyuOrderDO::getSourceUpdatedAt);
        query.isNotNull(XianyuOrderDO::getRentalOrderId)
                .likeLeft(XianyuOrderDO::getReceiverMobile, mobileLast4)
                .orderByDesc(XianyuOrderDO::getSourceUpdatedAt)
                .orderByDesc(XianyuOrderDO::getId);
        query.last("LIMIT 3");
        return query;
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

    default List<XianyuOrderDO> selectMissingRentalPeriodRefs(String currentParseVersion, int limit) {
        int boundedLimit = Math.max(1, Math.min(500, limit));
        LambdaQueryWrapperX<XianyuOrderDO> query = new LambdaQueryWrapperX<>();
        query.select(XianyuOrderDO::getId, XianyuOrderDO::getSellerRemark,
                XianyuOrderDO::getRemarkParseVersion,
                XianyuOrderDO::getBillableStartDate, XianyuOrderDO::getBillableEndDate,
                XianyuOrderDO::getShipDate, XianyuOrderDO::getReceiveDate, XianyuOrderDO::getReturnDate,
                XianyuOrderDO::getOrderTime, XianyuOrderDO::getSourceCreatedAt,
                XianyuOrderDO::getSourceUpdatedAt, XianyuOrderDO::getRawPayloadId);
        query.and(wrapper -> wrapper
                .isNull(XianyuOrderDO::getRentalPeriodStatus)
                .or().isNull(XianyuOrderDO::getRemarkParseVersion)
                .or().ne(XianyuOrderDO::getRemarkParseVersion, currentParseVersion));
        query.and(wrapper -> wrapper.isNull(XianyuOrderDO::getConversionStatus)
                .or().ne(XianyuOrderDO::getConversionStatus, "CONFIG_SKIPPED"));
        query.orderByDesc(XianyuOrderDO::getSourceUpdatedAt);
        query.orderByDesc(XianyuOrderDO::getId);
        query.last("LIMIT " + boundedLimit);
        return selectList(query);
    }

    default List<XianyuOrderDO> selectRemarkReparseCandidates(Long beforeId, int limit) {
        int boundedLimit = Math.max(1, Math.min(500, limit));
        LambdaQueryWrapperX<XianyuOrderDO> query = new LambdaQueryWrapperX<>();
        query.select(XianyuOrderDO::getId, XianyuOrderDO::getSellerRemark,
                XianyuOrderDO::getRemarkParseVersion,
                XianyuOrderDO::getBillableStartDate, XianyuOrderDO::getBillableEndDate,
                XianyuOrderDO::getShipDate, XianyuOrderDO::getReceiveDate, XianyuOrderDO::getReturnDate,
                XianyuOrderDO::getOrderTime, XianyuOrderDO::getSourceCreatedAt,
                XianyuOrderDO::getSourceUpdatedAt, XianyuOrderDO::getRawPayloadId);
        query.isNotNull(XianyuOrderDO::getSellerRemark)
                .ne(XianyuOrderDO::getSellerRemark, "")
                .and(wrapper -> wrapper.isNull(XianyuOrderDO::getRentalPeriodStatus)
                        .or().ne(XianyuOrderDO::getRentalPeriodStatus, "SUCCESS"))
                .and(wrapper -> wrapper.isNull(XianyuOrderDO::getConversionStatus)
                        .or().ne(XianyuOrderDO::getConversionStatus, "CONFIG_SKIPPED"));
        query.lt(beforeId != null, XianyuOrderDO::getId, beforeId);
        query.orderByDesc(XianyuOrderDO::getId);
        query.last("LIMIT " + boundedLimit);
        return selectList(query);
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

    @Select("""
            SELECT MAX(id)
              FROM xianyu_order
             WHERE tenant_id = #{tenantId}
               AND deleted = FALSE
            """)
    Long selectHistoricalReconciliationMaxId(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT id
              FROM xianyu_order
             WHERE tenant_id = #{tenantId}
               AND deleted = FALSE
               AND id > #{afterId}
               AND id <= #{endIdInclusive}
             ORDER BY id
             LIMIT #{limit}
            """)
    List<Long> selectHistoricalReconciliationCandidateIds(
            @Param("tenantId") Long tenantId,
            @Param("afterId") Long afterId,
            @Param("endIdInclusive") Long endIdInclusive,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT xo.id
              FROM xianyu_order xo
             WHERE xo.tenant_id = #{tenantId}
               AND xo.deleted = FALSE
               AND xo.shop_id = #{shopId}
               AND xo.xianyu_item_id = #{xianyuItemId}
               AND xo.rental_order_id IS NOT NULL
               AND xo.conversion_status &lt;&gt; 'CLOSED'
               AND NOT EXISTS (
                   SELECT 1
                     FROM rental_device_assignment assignment
                    WHERE assignment.tenant_id = xo.tenant_id
                      AND assignment.rental_order_id = xo.rental_order_id
                      AND assignment.deleted = FALSE
               )
               <if test="afterId != null">
               AND xo.id &gt; #{afterId}
               </if>
             ORDER BY xo.id
             LIMIT #{limit}
            </script>
            """)
    List<Long> selectMutableReconciliationCandidateIdsByItem(
            @Param("tenantId") Long tenantId,
            @Param("shopId") Long shopId,
            @Param("xianyuItemId") String xianyuItemId,
            @Param("afterId") Long afterId,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT xo.id
              FROM xianyu_order xo
             WHERE xo.tenant_id = #{tenantId}
               AND xo.deleted = FALSE
               AND xo.shop_id = #{shopId}
               AND xo.xgj_product_id = #{xgjProductId}
               AND xo.rental_order_id IS NOT NULL
               AND xo.conversion_status &lt;&gt; 'CLOSED'
               AND NOT EXISTS (
                   SELECT 1
                     FROM rental_device_assignment assignment
                    WHERE assignment.tenant_id = xo.tenant_id
                      AND assignment.rental_order_id = xo.rental_order_id
                      AND assignment.deleted = FALSE
               )
               <if test="afterId != null">
               AND xo.id &gt; #{afterId}
               </if>
             ORDER BY xo.id
             LIMIT #{limit}
            </script>
            """)
    List<Long> selectMutableReconciliationCandidateIdsByProduct(
            @Param("tenantId") Long tenantId,
            @Param("shopId") Long shopId,
            @Param("xgjProductId") String xgjProductId,
            @Param("afterId") Long afterId,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT xo.id
              FROM xianyu_order xo
             WHERE xo.tenant_id = #{tenantId}
               AND xo.deleted = FALSE
               AND xo.shop_id = #{shopId}
               AND xo.xgj_product_id = #{xgjProductId}
               AND xo.xgj_sku_id IN
               <foreach collection="xgjSkuIds" item="xgjSkuId" open="(" separator="," close=")">
                   #{xgjSkuId}
               </foreach>
               AND xo.rental_order_id IS NOT NULL
               AND xo.conversion_status &lt;&gt; 'CLOSED'
               AND NOT EXISTS (
                   SELECT 1
                     FROM rental_device_assignment assignment
                    WHERE assignment.tenant_id = xo.tenant_id
                      AND assignment.rental_order_id = xo.rental_order_id
                      AND assignment.deleted = FALSE
               )
               <if test="afterId != null">
               AND xo.id &gt; #{afterId}
               </if>
             ORDER BY xo.id
             LIMIT #{limit}
            </script>
            """)
    List<Long> selectMutableReconciliationCandidateIdsByProductAndSkus(
            @Param("tenantId") Long tenantId,
            @Param("shopId") Long shopId,
            @Param("xgjProductId") String xgjProductId,
            @Param("xgjSkuIds") Collection<String> xgjSkuIds,
            @Param("afterId") Long afterId,
            @Param("limit") int limit);

    default PageResult<XianyuOrderDO> selectPendingShipPage(Long shopId, String keyword, Collection<String> statuses,
                                                            cn.iocoder.yudao.framework.common.pojo.PageParam pageParam) {
        LambdaQueryWrapperX<XianyuOrderDO> query = pendingShipQuery(shopId, keyword, statuses);
        query.select(XianyuOrderDO.class, field -> !"detailJson".equals(field.getProperty())
                && !"goodsJson".equals(field.getProperty())
                && !"payNo".equals(field.getProperty()));
        return selectPage(pageParam, query);
    }

    static LambdaQueryWrapperX<XianyuOrderDO> pendingShipQuery(Long shopId, String keyword,
                                                               Collection<String> statuses) {
        LambdaQueryWrapperX<XianyuOrderDO> query = new LambdaQueryWrapperX<>();
        query.eqIfPresent(XianyuOrderDO::getShopId, shopId);
        query.in(statuses != null && !statuses.isEmpty(), XianyuOrderDO::getOrderStatus, statuses);
        // Official order data uses both null and empty strings for an unassigned waybill.
        query.apply("(waybill_no IS NULL OR TRIM(waybill_no) = '')");
        query.isNull(XianyuOrderDO::getConsignTime);
        query.isNull(XianyuOrderDO::getCancelTime);
        query.orderByDesc(XianyuOrderDO::getOrderTime);
        query.orderByDesc(XianyuOrderDO::getId);
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(XianyuOrderDO::getExternalOrderId, keyword)
                    .or().like(XianyuOrderDO::getReceiverName, keyword)
                    .or().like(XianyuOrderDO::getReceiverMobile, keyword)
                    .or().like(XianyuOrderDO::getBuyerNick, keyword)
                    .or().like(XianyuOrderDO::getGoodsTitle, keyword));
        }
        return query;
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

    default int updateEffectivePlanById(Long id,
                                        LocalDate billableStartDate,
                                        LocalDate billableEndDate,
                                        LocalDate shipDate,
                                        LocalDate receiveDate,
                                        LocalDate returnDate) {
        return update(null, new LambdaUpdateWrapper<XianyuOrderDO>()
                .eq(XianyuOrderDO::getId, id)
                .set(XianyuOrderDO::getBillableStartDate, billableStartDate)
                .set(XianyuOrderDO::getBillableEndDate, billableEndDate)
                .set(XianyuOrderDO::getShipDate, shipDate)
                .set(XianyuOrderDO::getReceiveDate, receiveDate)
                .set(XianyuOrderDO::getReturnDate, returnDate)
                .set(XianyuOrderDO::getUpdater, "system"));
    }

}
