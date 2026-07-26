package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncWindow;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.service.RentalConversionResult;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
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
    private final XianyuOrderSyncService orderSyncService;
    private final XianyuRentalConversionService conversionService;
    private final ObjectMapper objectMapper;

    public XianyuOrderAdminService(XianyuOrderMapper orderMapper, XianyuShopMapper shopMapper,
                                   XianyuOrderSyncService orderSyncService,
                                   XianyuRentalConversionService conversionService,
                                   ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.orderSyncService = orderSyncService;
        this.conversionService = conversionService;
        this.objectMapper = objectMapper;
    }

    public PageResult<XianyuOrderRespVO> getOrderPage(XianyuOrderPageReqVO pageReqVO) {
        PageResult<XianyuOrderDO> page = orderMapper.selectAdminPage(pageReqVO);
        List<XianyuOrderRespVO> list = page.getList().stream().map(this::toVo).collect(Collectors.toList());
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

    public RentalConversionResult convert(Long channelOrderId) {
        return conversionService.convert(channelOrderId);
    }

    private XianyuOrderRespVO toVo(XianyuOrderDO order) {
        XianyuOrderRespVO vo = new XianyuOrderRespVO();
        vo.setId(order.getId());
        vo.setShopId(order.getShopId());
        // Ops needs full order no. for lookup / print; do not redact.
        vo.setExternalOrderId(order.getExternalOrderId());
        vo.setExternalProductId(order.getExternalProductId());
        vo.setExternalSkuId(order.getExternalSkuId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayAmount(order.getPayAmount());
        vo.setCurrency(order.getCurrency());
        // Seller remark may contain ship dates; keep full text for ops (no phone/address strip).
        vo.setSellerRemark(order.getSellerRemark());
        fillReceiverFromDetail(order.getDetailJson(), vo);
        vo.setRemarkParseStatus(order.getRemarkParseStatus());
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
        return vo;
    }

    /**
     * Surface shipping contact from stored detail JSON. Never attach raw detailJson to the VO.
     */
    private void fillReceiverFromDetail(String detailJson, XianyuOrderRespVO vo) {
        if (!StringUtils.hasText(detailJson)) {
            return;
        }
        try {
            JsonNode detail = objectMapper.readTree(detailJson);
            vo.setReceiverName(textOrNull(detail, "receiver_name"));
            vo.setReceiverMobile(textOrNull(detail, "receiver_mobile"));
            vo.setReceiverAddress(composeAddress(detail));
        } catch (Exception ignored) {
            // Malformed historical payload: leave receiver fields empty.
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String composeAddress(JsonNode detail) {
        List<String> parts = new ArrayList<>(5);
        for (String field : List.of("prov_name", "city_name", "area_name", "town_name", "address")) {
            String part = textOrNull(detail, field);
            if (part != null) {
                parts.add(part);
            }
        }
        return parts.isEmpty() ? null : String.join("", parts);
    }

}
