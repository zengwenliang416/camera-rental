package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncWindow;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationResult;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ORDER_SYNC_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZE_MISSING;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;

@Service
public class XianyuOrderAdminService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuOrderMapper orderMapper;
    private final XianyuShopMapper shopMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final RentalOrderItemMapper rentalOrderItemMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final XianyuOrderSyncService orderSyncService;
    private final RentalChannelOrderReconciliationService reconciliationService;

    public XianyuOrderAdminService(XianyuOrderMapper orderMapper, XianyuShopMapper shopMapper,
                                   RentalOrderMapper rentalOrderMapper,
                                   RentalOrderItemMapper rentalOrderItemMapper,
                                   RentalDeviceAssignmentMapper assignmentMapper,
                                   XianyuOrderSyncService orderSyncService,
                                   RentalChannelOrderReconciliationService reconciliationService) {
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.rentalOrderItemMapper = rentalOrderItemMapper;
        this.assignmentMapper = assignmentMapper;
        this.orderSyncService = orderSyncService;
        this.reconciliationService = reconciliationService;
    }

    public PageResult<XianyuOrderRespVO> getOrderPage(XianyuOrderPageReqVO pageReqVO) {
        PageResult<XianyuOrderDO> page = orderMapper.selectAdminPage(pageReqVO);
        List<Long> rentalOrderIds = page.getList().stream()
                .map(XianyuOrderDO::getRentalOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<RentalOrderItemDO> items = rentalOrderIds.isEmpty()
                ? List.of()
                : nullSafe(rentalOrderItemMapper.selectListByRentalOrderIds(rentalOrderIds));
        Map<Long, RentalOrderItemDO> firstItemByOrderId = items.stream().collect(Collectors.toMap(
                RentalOrderItemDO::getRentalOrderId,
                item -> item,
                (first, ignored) -> first,
                LinkedHashMap::new));
        Map<Long, RentalOrderDO> rentalOrderById = rentalOrderIds.isEmpty()
                ? Map.of()
                : nullSafe(rentalOrderMapper.selectByIds(rentalOrderIds)).stream()
                .collect(Collectors.toMap(RentalOrderDO::getId, order -> order));
        Map<Long, List<Long>> deviceIdsByOrderItemId = new LinkedHashMap<>();
        if (!rentalOrderIds.isEmpty()) {
            nullSafe(assignmentMapper.selectActiveListByRentalOrderIds(rentalOrderIds)).forEach(assignment ->
                    deviceIdsByOrderItemId.computeIfAbsent(
                            assignment.getRentalOrderItemId(), ignored -> new ArrayList<>())
                            .add(assignment.getDeviceId()));
        }
        List<XianyuOrderRespVO> list = page.getList().stream()
                .map(order -> {
                    RentalOrderItemDO item = firstItemByOrderId.get(order.getRentalOrderId());
                    List<Long> assignedDeviceIds = item == null
                            ? List.of()
                            : deviceIdsByOrderItemId.getOrDefault(item.getId(), List.of());
                    return toVo(order, item, assignedDeviceIds,
                            order.getRentalOrderId() == null
                                    ? null
                                    : rentalOrderById.get(order.getRentalOrderId()));
                })
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    public XianyuOrderSyncRespVO syncPage(XianyuOrderSyncReqVO reqVO) {
        XianyuShopDO shop = shopMapper.selectByTenantIdAndId(
                TenantContextHolder.getRequiredTenantId(), reqVO.getShopId());
        if (shop == null) {
            throw exception(XIANYU_SHOP_NOT_EXISTS);
        }
        if (!StringUtils.hasText(shop.getAuthorizeId())) {
            throw exception(XIANYU_SHOP_AUTHORIZE_MISSING);
        }
        if (!"VALID".equals(shop.getAuthorizationStatus())
                || shop.getAuthorizationExpiresAt() != null
                && !shop.getAuthorizationExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw exception(XIANYU_SHOP_AUTHORIZATION_INVALID);
        }
        try {
            XianyuOrderSyncWindow window = new XianyuOrderSyncWindow(
                    reqVO.getWindowStart(), reqVO.getWindowEnd(), reqVO.getPageNo(), reqVO.getPageSize());
            XianyuOrderPageSyncResult result = orderSyncService.syncPage(
                    shop.getId(), Long.valueOf(shop.getAuthorizeId()), window);
            XianyuOrderSyncRespVO resp = new XianyuOrderSyncRespVO();
            resp.setSyncRunId(result.syncRunId());
            resp.setReceivedCount(result.receivedCount());
            resp.setSucceededCount(result.succeededCount());
            resp.setCursorAdvanced(result.cursorAdvanced());
            return resp;
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            String detail = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
            throw exception(XIANYU_ORDER_SYNC_FAILED, detail);
        }
    }

    public RentalChannelOrderReconciliationResult convert(Long channelOrderId) {
        return reconciliationService.reconcile(channelOrderId);
    }

    private XianyuOrderRespVO toVo(XianyuOrderDO order, RentalOrderItemDO item,
                                   List<Long> assignedDeviceIds, RentalOrderDO rentalOrder) {
        XianyuOrderRespVO vo = new XianyuOrderRespVO();
        vo.setId(order.getId());
        vo.setShopId(order.getShopId());
        // Ops needs full order no. for lookup / print; do not redact.
        vo.setExternalOrderId(order.getExternalOrderId());
        vo.setXgjProductId(order.getXgjProductId());
        vo.setXianyuItemId(order.getXianyuItemId());
        vo.setXgjSkuId(order.getXgjSkuId());
        vo.setXianyuSkuId(order.getXianyuSkuId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayAmount(order.getPayAmount());
        vo.setCurrency(order.getCurrency());
        vo.setSellerRemark(order.getSellerRemark());
        fillReceiver(order, vo);
        vo.setBuyerNick(order.getBuyerNick());
        vo.setRemarkParseVersion(order.getRemarkParseVersion());
        vo.setRemarkParseStatus(order.getRemarkParseStatus());
        vo.setRemarkParseSource(order.getRemarkParseSource());
        vo.setRemarkParseConfidence(order.getRemarkParseConfidence());
        vo.setRemarkParseModel(order.getRemarkParseModel());
        vo.setShipDate(order.getShipDate());
        fillRentalPeriod(order, rentalOrder, vo);
        vo.setConversionStatus(order.getConversionStatus());
        vo.setRentalOrderId(order.getRentalOrderId());
        vo.setSourceCreatedAt(order.getSourceCreatedAt());
        vo.setSourceUpdatedAt(order.getSourceUpdatedAt());
        vo.setOrderType(order.getOrderType());
        vo.setOrderTime(order.getOrderTime());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayTime(order.getPayTime());
        vo.setRefundStatus(order.getRefundStatus());
        vo.setRefundAmount(order.getRefundAmount());
        vo.setRefundTime(order.getRefundTime());
        vo.setWaybillNo(order.getWaybillNo());
        vo.setExpressCode(order.getExpressCode());
        vo.setExpressName(order.getExpressName());
        vo.setExpressFee(order.getExpressFee());
        vo.setConsignType(order.getConsignType());
        vo.setConsignTime(order.getConsignTime());
        vo.setConfirmTime(order.getConfirmTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setCancelTime(order.getCancelTime());
        vo.setSellerName(order.getSellerName());
        vo.setGoodsTitle(order.getGoodsTitle());
        vo.setGoodsQuantity(order.getGoodsQuantity());
        vo.setGoodsPrice(order.getGoodsPrice());
        vo.setXybSellerAmount(order.getXybSellerAmount());
        vo.setTaxIncluded(order.getTaxIncluded());
        vo.setIdleBizType(order.getIdleBizType());
        vo.setPinGroupStatus(order.getPinGroupStatus());
        if (item != null) {
            vo.setRentalOrderItemId(item.getId());
            vo.setEquipmentModelCode(item.getEquipmentModelCode());
            vo.setRentalQuantity(item.getQuantity());
            vo.setOccupyStartDate(item.getOccupyStartDate());
            vo.setOccupyEndDateExclusive(item.getOccupyEndDateExclusive());
        } else if (order.getShipDate() != null && order.getReturnDate() != null) {
            vo.setOccupyStartDate(order.getShipDate());
            vo.setOccupyEndDateExclusive(order.getReturnDate().plusDays(1));
        }
        vo.setAssignedDeviceIds(List.copyOf(assignedDeviceIds));
        return vo;
    }

    private static <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private void fillRentalPeriod(XianyuOrderDO order, RentalOrderDO rentalOrder,
                                  XianyuOrderRespVO vo) {
        if (rentalOrder != null && rentalOrder.getBillableStartDate() != null
                && rentalOrder.getBillableEndDate() != null) {
            vo.setBillableStartDate(rentalOrder.getBillableStartDate());
            vo.setBillableEndDate(rentalOrder.getBillableEndDate());
            vo.setRentalPeriodStatus("SUCCESS");
            return;
        }

        vo.setBillableStartDate(order.getBillableStartDate());
        vo.setBillableEndDate(order.getBillableEndDate());
        vo.setRentalPeriodStatus(order.getRentalPeriodStatus());
        vo.setRentalPeriodReasonCode(order.getRentalPeriodReasonCode());
    }

    private void fillReceiver(XianyuOrderDO order, XianyuOrderRespVO vo) {
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverMobile(order.getReceiverMobile());
        vo.setReceiverAddress(order.getReceiverAddress());
    }

}
