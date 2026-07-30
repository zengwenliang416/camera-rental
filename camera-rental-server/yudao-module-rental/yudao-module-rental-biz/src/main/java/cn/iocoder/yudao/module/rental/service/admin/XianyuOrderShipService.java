package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceShipmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceShipmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuWriteClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuWriteEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import cn.iocoder.yudao.module.rental.service.RentalConversionResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_DEVICE_NOT_SHIPPABLE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_IDEMPOTENT_KEY_REUSED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_ORDER_NOT_CONVERTED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_ORDER_NOT_PENDING;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_REMOTE_ERROR;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_WRITE_DISABLED;

@Service
public class XianyuOrderShipService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> PENDING_STATUSES = Set.of("12", "WAIT_SHIP", "WAIT_SEND", "WAIT_SELLER_SEND_GOODS");

    private final XianyuOrderMapper orderMapper;
    private final XianyuShopMapper shopMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final RentalOrderItemMapper rentalOrderItemMapper;
    private final RentalDeviceShipmentMapper shipmentMapper;
    private final XianyuRentalConversionService conversionService;
    private final RentalDeviceAssignmentService assignmentService;
    private final RentalDeviceOpsService deviceOpsService;
    private final XianyuWriteClient writeClient;
    private final XianyuRuntimeConfigService runtimeConfigService;
    private final ObjectMapper objectMapper;

    public XianyuOrderShipService(XianyuOrderMapper orderMapper, XianyuShopMapper shopMapper,
                                  RentalDeviceMapper deviceMapper, RentalDeviceAssignmentMapper assignmentMapper,
                                  RentalOrderMapper rentalOrderMapper,
                                  RentalOrderItemMapper rentalOrderItemMapper,
                                  RentalDeviceShipmentMapper shipmentMapper,
                                  XianyuRentalConversionService conversionService,
                                  RentalDeviceAssignmentService assignmentService,
                                  RentalDeviceOpsService deviceOpsService,
                                  XianyuWriteClient writeClient, XianyuRuntimeConfigService runtimeConfigService,
                                  ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.deviceMapper = deviceMapper;
        this.assignmentMapper = assignmentMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.rentalOrderItemMapper = rentalOrderItemMapper;
        this.shipmentMapper = shipmentMapper;
        this.conversionService = conversionService;
        this.assignmentService = assignmentService;
        this.deviceOpsService = deviceOpsService;
        this.writeClient = writeClient;
        this.runtimeConfigService = runtimeConfigService;
        this.objectMapper = objectMapper;
    }

    public PageResult<XianyuPendingShipOrderRespVO> searchPendingOrders(XianyuPendingShipOrderPageReqVO reqVO) {
        PageResult<XianyuOrderDO> page = orderMapper.selectPendingShipPage(reqVO.getShopId(), reqVO.getKeyword(),
                PENDING_STATUSES, reqVO);
        List<XianyuPendingShipOrderRespVO> list = page.getList().stream()
                .map(this::toPendingVo)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public XianyuOrderShipRespVO ship(XianyuOrderShipReqVO reqVO) {
        RentalDeviceShipmentDO replay = shipmentMapper.selectByIdempotencyKeyForUpdate(reqVO.getIdempotencyKey());
        if (replay != null) {
            return toShipResp(replay, deviceMapper.selectById(replay.getDeviceId()), "DISPATCHED");
        }
        XianyuProperties properties = runtimeConfigService.getCurrent();
        if (properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY
                || !properties.isWriteEnabled()) {
            throw exception(XIANYU_WRITE_DISABLED);
        }

        XianyuOrderDO order = orderMapper.selectByIdForUpdate(reqVO.getChannelOrderId());
        if (order == null) {
            throw exception(XIANYU_ORDER_NOT_EXISTS);
        }
        requirePending(order);
        XianyuShopDO shop = requireAuthorizedShop(order.getShopId());
        RentalDeviceDO device = resolveDevice(reqVO);
        requireDeviceShippable(device);
        RentalOrderItemDO item = requireConvertedFirstItem(order, device);

        RentalDeviceAssignmentResult assignment = assignDevice(reqVO, device, item);
        ObjectNode shipBody = buildShipBody(order, reqVO);
        XianyuReadResponse remote = callRemote(shipBody);

        RentalDeviceOpsRespVO dispatched = dispatch(device.getId(), assignment.assignmentId());
        RentalDeviceShipmentDO shipment = RentalDeviceShipmentDO.builder()
                .channelOrderId(order.getId())
                .assignmentId(assignment.assignmentId())
                .deviceId(device.getId())
                .idempotencyKey(reqVO.getIdempotencyKey())
                .waybillNo(reqVO.getWaybillNo())
                .expressCode(reqVO.getExpressCode())
                .expressName(reqVO.getExpressName())
                .shipRequestHash(DigestUtils.md5DigestAsHex(shipBody.toString().getBytes(StandardCharsets.UTF_8)))
                .shipResponseCode(remote.remoteCode())
                .shipResponseMsg(remote.payload().path("msg").asText("ok"))
                .ocrConfirmed(Boolean.TRUE.equals(reqVO.getOcrConfirmed()))
                .source(reqVO.getSource())
                .build();
        shipmentMapper.insert(shipment);

        order.setWaybillNo(reqVO.getWaybillNo());
        order.setExpressCode(reqVO.getExpressCode());
        order.setExpressName(reqVO.getExpressName());
        order.setConsignTime(LocalDateTime.now(BUSINESS_ZONE));
        orderMapper.updateById(order);

        XianyuOrderShipRespVO resp = toShipResp(shipment, device, dispatched.getAssignmentStatus());
        resp.setRemoteMsg(shipment.getShipResponseMsg());
        return resp;
    }

    private XianyuPendingShipOrderRespVO toPendingVo(XianyuOrderDO order) {
        XianyuPendingShipOrderRespVO vo = new XianyuPendingShipOrderRespVO();
        vo.setId(order.getId());
        vo.setShopId(order.getShopId());
        vo.setExternalOrderId(order.getExternalOrderId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setGoodsTitle(order.getGoodsTitle());
        vo.setGoodsQuantity(order.getGoodsQuantity());
        vo.setPayAmount(order.getPayAmount());
        vo.setBuyerNick(order.getBuyerNick());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverMobile(order.getReceiverMobile());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setSellerRemark(order.getSellerRemark());
        vo.setRentalOrderId(order.getRentalOrderId());
        vo.setConversionStatus(order.getConversionStatus());
        vo.setOrderTime(order.getOrderTime());
        vo.setSourceUpdatedAt(order.getSourceUpdatedAt());
        return vo;
    }

    private void requirePending(XianyuOrderDO order) {
        if (StringUtils.hasText(order.getWaybillNo()) || order.getConsignTime() != null || order.getCancelTime() != null
                || StringUtils.hasText(order.getOrderStatus()) && !PENDING_STATUSES.contains(order.getOrderStatus())) {
            throw exception(XIANYU_SHIP_ORDER_NOT_PENDING);
        }
    }

    private XianyuShopDO requireAuthorizedShop(Long shopId) {
        XianyuShopDO shop = shopMapper.selectByTenantIdAndId(TenantContextHolder.getRequiredTenantId(), shopId);
        if (shop == null) {
            throw exception(XIANYU_SHOP_NOT_EXISTS);
        }
        if (!"VALID".equals(shop.getAuthorizationStatus())
                || shop.getAuthorizationExpiresAt() != null
                && !shop.getAuthorizationExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw exception(XIANYU_SHOP_AUTHORIZATION_INVALID);
        }
        return shop;
    }

    private RentalDeviceDO resolveDevice(XianyuOrderShipReqVO reqVO) {
        RentalDeviceDO device = reqVO.getDeviceId() != null
                ? deviceMapper.selectByIdForUpdate(reqVO.getDeviceId())
                : deviceMapper.selectByDeviceNoForUpdate(reqVO.getDeviceNo());
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        return device;
    }

    private void requireDeviceShippable(RentalDeviceDO device) {
        if (!Boolean.TRUE.equals(device.getEnabled())) {
            throw exception(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE, "设备已停用");
        }
        if (!"AVAILABLE".equals(device.getStatus())) {
            throw exception(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE, "设备状态：" + device.getStatus());
        }
    }

    private RentalOrderItemDO requireConvertedFirstItem(XianyuOrderDO order, RentalDeviceDO device) {
        Long rentalOrderId = order.getRentalOrderId();
        if (rentalOrderId == null) {
            RentalConversionResult conversion =
                    conversionService.convertForShipment(order.getId(), device.getEquipmentModelCode());
            if (!"CONVERTED".equals(conversion.status()) || conversion.rentalOrderId() == null) {
                throw exception(XIANYU_SHIP_ORDER_NOT_CONVERTED);
            }
            rentalOrderId = conversion.rentalOrderId();
            order.setRentalOrderId(rentalOrderId);
            order.setConversionStatus("CONVERTED");
        }
        RentalOrderDO rentalOrder = rentalOrderMapper.selectByIdForUpdate(rentalOrderId);
        if (rentalOrder == null) {
            throw exception(XIANYU_SHIP_ORDER_NOT_CONVERTED);
        }
        RentalOrderItemDO item = rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(rentalOrder.getId());
        if (item == null || item.getOccupyStartDate() == null || item.getOccupyEndDateExclusive() == null) {
            throw exception(XIANYU_SHIP_ORDER_NOT_CONVERTED);
        }
        return item;
    }

    private RentalDeviceAssignmentResult assignDevice(XianyuOrderShipReqVO reqVO, RentalDeviceDO device,
                                                       RentalOrderItemDO item) {
        RentalDeviceAssignmentDO existing =
                assignmentMapper.selectActiveByOrderItemAndDeviceForUpdate(item.getId(), device.getId());
        if (existing != null) {
            return new RentalDeviceAssignmentResult(existing.getId(), existing.getScheduleId(), device.getId(),
                    item.getOccupyStartDate(), item.getOccupyEndDateExclusive());
        }
        try {
            return assignmentService.assign(new RentalDeviceAssignmentCommand(
                    item.getId(), device.getId(), item.getOccupyStartDate(), item.getOccupyEndDateExclusive(),
                    "ship:" + reqVO.getIdempotencyKey()));
        } catch (RentalDeviceAssignmentException ex) {
            if (ex.getCode() == RentalDeviceAssignmentException.Code.IDEMPOTENCY_KEY_REUSED) {
                throw exception(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED);
            }
            throw exception(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE, ex.getCode().name());
        }
    }

    private ObjectNode buildShipBody(XianyuOrderDO order, XianyuOrderShipReqVO reqVO) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("order_no", order.getExternalOrderId());
        body.put("waybill_no", reqVO.getWaybillNo());
        body.put("express_code", reqVO.getExpressCode());
        body.put("express_name", reqVO.getExpressName());
        return body;
    }

    private XianyuReadResponse callRemote(ObjectNode shipBody) {
        try {
            return writeClient.execute(XianyuWriteEndpoint.ORDER_SHIP, shipBody);
        } catch (XianyuClientException ex) {
            throw exception(XIANYU_SHIP_REMOTE_ERROR, ex.getKind().name());
        }
    }

    private RentalDeviceOpsRespVO dispatch(Long deviceId, Long assignmentId) {
        RentalDeviceDispatchReqVO reqVO = new RentalDeviceDispatchReqVO();
        reqVO.setDeviceId(deviceId);
        reqVO.setAssignmentId(assignmentId);
        return deviceOpsService.dispatch(reqVO);
    }

    private XianyuOrderShipRespVO toShipResp(RentalDeviceShipmentDO shipment, RentalDeviceDO device,
                                              String assignmentStatus) {
        XianyuOrderShipRespVO resp = new XianyuOrderShipRespVO();
        resp.setShipmentId(shipment.getId());
        resp.setChannelOrderId(shipment.getChannelOrderId());
        resp.setAssignmentId(shipment.getAssignmentId());
        resp.setDeviceId(shipment.getDeviceId());
        resp.setDeviceNo(device == null ? null : device.getDeviceNo());
        resp.setMaskedWaybillNo(maskWaybill(shipment.getWaybillNo()));
        resp.setExpressCode(shipment.getExpressCode());
        resp.setExpressName(shipment.getExpressName());
        resp.setRemoteCode(shipment.getShipResponseCode());
        resp.setRemoteMsg(shipment.getShipResponseMsg());
        resp.setAssignmentStatus(assignmentStatus);
        resp.setSource(shipment.getSource());
        return resp;
    }

    private static String maskWaybill(String waybillNo) {
        if (!StringUtils.hasText(waybillNo) || waybillNo.length() <= 6) {
            return "****";
        }
        return waybillNo.substring(0, Math.min(2, waybillNo.length())) + "****"
                + waybillNo.substring(waybillNo.length() - 4);
    }

}
