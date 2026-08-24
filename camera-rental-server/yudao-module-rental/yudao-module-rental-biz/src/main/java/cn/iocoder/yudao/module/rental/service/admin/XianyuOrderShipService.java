package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderDispatchBackfillReqVO;
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
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryCreateCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryDeviceCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryService;
import cn.iocoder.yudao.module.rental.service.logistics.WaybillPrivacy;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryDirectionEnum;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_DISPATCH_BACKFILL_CONFLICT;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_DISPATCH_BACKFILL_ORDER_NOT_SHIPPED;
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
    private static final Set<String> BACKFILL_SHIPPED_STATUSES = Set.of("21", "22");
    private static final Set<String> BACKFILL_CLOSED_STATUSES = Set.of("23", "24");
    private static final Integer BACKFILL_REFUND_SUCCESS_STATUS = 5;
    private static final String BACKFILL_SOURCE = "ADMIN_BACKFILL";

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
    private final RentalDeliveryService deliveryService;
    private final WaybillPrivacy waybillPrivacy;
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
                                  RentalDeliveryService deliveryService,
                                  WaybillPrivacy waybillPrivacy,
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
        this.deliveryService = deliveryService;
        this.waybillPrivacy = waybillPrivacy;
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
            RentalDeliveryResult tracking = replay.getDeliveryId() == null
                    ? null : deliveryService.getResult(replay.getDeliveryId());
            return toShipResp(replay, deviceMapper.selectById(replay.getDeviceId()), "DISPATCHED", tracking);
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

        String sourceIdentifier = "shipment:"
                + DigestUtils.md5DigestAsHex(reqVO.getIdempotencyKey().getBytes(StandardCharsets.UTF_8));
        RentalDeliveryResult delivery = deliveryService.createOrReuse(buildDeliveryCommand(
                sourceIdentifier, reqVO.getExpressCode(), reqVO.getExpressName(), reqVO.getWaybillNo(),
                order, item, assignment, device));
        shipment.setDeliveryId(delivery.deliveryId());
        shipmentMapper.updateById(shipment);

        order.setWaybillNo(reqVO.getWaybillNo());
        order.setExpressCode(reqVO.getExpressCode());
        order.setExpressName(reqVO.getExpressName());
        order.setConsignTime(LocalDateTime.now(BUSINESS_ZONE));
        orderMapper.updateById(order);

        XianyuOrderShipRespVO resp = toShipResp(shipment, device, dispatched.getAssignmentStatus(), delivery);
        resp.setRemoteMsg(shipment.getShipResponseMsg());
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public XianyuOrderShipRespVO backfillDispatch(XianyuOrderDispatchBackfillReqVO reqVO) {
        String idempotencyKey = reqVO.getIdempotencyKey().trim();
        String waybillNo = reqVO.getWaybillNo().trim();
        String expressCode = reqVO.getExpressCode().trim();
        String expressName = reqVO.getExpressName().trim();
        RentalDeviceShipmentDO replay = shipmentMapper.selectByIdempotencyKeyForUpdate(idempotencyKey);
        if (replay != null) {
            requireBackfillReplayMatches(replay, reqVO, waybillNo, expressCode);
            RentalDeviceDO replayDevice = requireBackfillReplayDeviceMatches(replay, reqVO);
            if (!Objects.equals(replay.getShipRequestHash(),
                    backfillRequestHash(reqVO, replay.getDeviceId(), waybillNo, expressCode, expressName))) {
                throw exception(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED);
            }
            return replayResponse(replay, replayDevice);
        }

        XianyuOrderDO order = orderMapper.selectByIdForUpdate(reqVO.getChannelOrderId());
        if (order == null) {
            throw exception(XIANYU_ORDER_NOT_EXISTS);
        }
        requireBackfillEligible(order);
        requireTenantShop(order.getShopId());

        RentalDeviceDO device = resolveDevice(reqVO.getDeviceId(), reqVO.getDeviceNo());
        RentalDeviceShipmentDO existing = shipmentMapper.selectByBusinessKeyForUpdate(
                order.getId(), waybillNo, expressCode);
        if (existing != null) {
            String requestHash = backfillRequestHash(
                    reqVO, device.getId(), waybillNo, expressCode, expressName);
            if (!BACKFILL_SOURCE.equals(existing.getSource())
                    || !Objects.equals(existing.getDeviceId(), device.getId())
                    || !Objects.equals(existing.getShipRequestHash(), requestHash)
                    || existing.getDeliveryId() == null) {
                throw exception(XIANYU_DISPATCH_BACKFILL_CONFLICT, "同一运单已存在不兼容的出库记录");
            }
            return replayResponse(existing);
        }

        RentalOrderItemDO item = requireConvertedFirstItem(order, device);
        BackfillDispatchResult local = ensureBackfillDispatched(idempotencyKey, device, item);
        RentalDeviceShipmentDO shipment = RentalDeviceShipmentDO.builder()
                .channelOrderId(order.getId())
                .assignmentId(local.assignment().assignmentId())
                .deviceId(device.getId())
                .idempotencyKey(idempotencyKey)
                .waybillNo(waybillNo)
                .expressCode(expressCode)
                .expressName(expressName)
                .shipRequestHash(backfillRequestHash(
                        reqVO, device.getId(), waybillNo, expressCode, expressName))
                .shipResponseMsg("已发货补录：" + reqVO.getReason().trim())
                .ocrConfirmed(false)
                .source(BACKFILL_SOURCE)
                .build();
        shipmentMapper.insert(shipment);

        String sourceIdentifier = "shipment-backfill:"
                + DigestUtils.md5DigestAsHex(idempotencyKey.getBytes(StandardCharsets.UTF_8));
        RentalDeliveryResult delivery = deliveryService.createOrReuse(buildDeliveryCommand(
                sourceIdentifier, expressCode, expressName, waybillNo,
                order, item, local.assignment(), device));
        shipment.setDeliveryId(delivery.deliveryId());
        shipmentMapper.updateById(shipment);

        order.setWaybillNo(waybillNo);
        order.setExpressCode(expressCode);
        order.setExpressName(expressName);
        order.setConsignTime(reqVO.getConsignTime());
        if (order.getShipDate() == null) {
            order.setShipDate(reqVO.getConsignTime().toLocalDate());
        }
        orderMapper.updateById(order);

        return toShipResp(shipment, device, local.assignmentStatus(), delivery);
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

    private void requireTenantShop(Long shopId) {
        if (shopMapper.selectByTenantIdAndId(TenantContextHolder.getRequiredTenantId(), shopId) == null) {
            throw exception(XIANYU_SHOP_NOT_EXISTS);
        }
    }

    private RentalDeviceDO resolveDevice(XianyuOrderShipReqVO reqVO) {
        return resolveDevice(reqVO.getDeviceId(), reqVO.getDeviceNo());
    }

    private RentalDeviceDO resolveDevice(Long deviceId, String deviceNo) {
        RentalDeviceDO device = deviceId != null
                ? deviceMapper.selectByIdForUpdate(deviceId)
                : deviceMapper.selectByDeviceNoForUpdate(deviceNo.trim());
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        return device;
    }

    private void requireBackfillEligible(XianyuOrderDO order) {
        if (order.getCancelTime() != null
                || BACKFILL_CLOSED_STATUSES.contains(order.getOrderStatus())
                || Objects.equals(order.getRefundStatus(), BACKFILL_REFUND_SUCCESS_STATUS)) {
            throw exception(XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED);
        }
        if (!BACKFILL_SHIPPED_STATUSES.contains(order.getOrderStatus())) {
            throw exception(XIANYU_DISPATCH_BACKFILL_ORDER_NOT_SHIPPED);
        }
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

    private BackfillDispatchResult ensureBackfillDispatched(String idempotencyKey,
                                                            RentalDeviceDO device, RentalOrderItemDO item) {
        RentalDeviceAssignmentDO existing =
                assignmentMapper.selectActiveByOrderItemAndDeviceForUpdate(item.getId(), device.getId());
        if (existing != null) {
            RentalDeviceAssignmentResult assignment = new RentalDeviceAssignmentResult(
                    existing.getId(), existing.getScheduleId(), device.getId(),
                    item.getOccupyStartDate(), item.getOccupyEndDateExclusive());
            if ("DISPATCHED".equals(existing.getStatus())) {
                if (!"RENTED".equals(device.getStatus())) {
                    throw exception(XIANYU_DISPATCH_BACKFILL_CONFLICT, "已出库分配与设备状态不一致");
                }
                return new BackfillDispatchResult(assignment, "DISPATCHED");
            }
            if (!"ASSIGNED".equals(existing.getStatus())) {
                throw exception(XIANYU_DISPATCH_BACKFILL_CONFLICT, "当前设备分配状态不可补录出库");
            }
            requireDeviceShippable(device);
            RentalDeviceOpsRespVO dispatched = dispatch(device.getId(), existing.getId());
            return new BackfillDispatchResult(assignment, dispatched.getAssignmentStatus());
        }

        requireDeviceShippable(device);
        RentalDeviceAssignmentResult assignment = assignDevice(
                idempotencyKey, device, item, "backfill:");
        RentalDeviceOpsRespVO dispatched = dispatch(device.getId(), assignment.assignmentId());
        return new BackfillDispatchResult(assignment, dispatched.getAssignmentStatus());
    }

    private RentalDeviceAssignmentResult assignDevice(String idempotencyKey, RentalDeviceDO device,
                                                       RentalOrderItemDO item, String prefix) {
        try {
            return assignmentService.assign(new RentalDeviceAssignmentCommand(
                    item.getId(), device.getId(), item.getOccupyStartDate(), item.getOccupyEndDateExclusive(),
                    prefix + idempotencyKey));
        } catch (RentalDeviceAssignmentException ex) {
            if (ex.getCode() == RentalDeviceAssignmentException.Code.IDEMPOTENCY_KEY_REUSED) {
                throw exception(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED);
            }
            throw exception(XIANYU_DISPATCH_BACKFILL_CONFLICT, ex.getCode().name());
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

    private RentalDeliveryCreateCommand buildDeliveryCommand(String sourceIdentifier, String expressCode,
                                                              String expressName, String waybillNo,
                                                              XianyuOrderDO order, RentalOrderItemDO item,
                                                              RentalDeviceAssignmentResult assignment,
                                                              RentalDeviceDO device) {
        return new RentalDeliveryCreateCommand(item.getRentalOrderId(), order.getId(),
                RentalDeliveryDirectionEnum.OUTBOUND,
                "XIANYU", sourceIdentifier, expressCode, expressName,
                waybillNo, order.getReceiverMobile(),
                List.of(new RentalDeliveryDeviceCommand(item.getId(), assignment.assignmentId(), device.getId())));
    }

    private void requireBackfillReplayMatches(RentalDeviceShipmentDO replay,
                                              XianyuOrderDispatchBackfillReqVO reqVO,
                                              String waybillNo, String expressCode) {
        if (!BACKFILL_SOURCE.equals(replay.getSource())
                || !Objects.equals(replay.getChannelOrderId(), reqVO.getChannelOrderId())
                || !Objects.equals(replay.getWaybillNo(), waybillNo)
                || !Objects.equals(replay.getExpressCode(), expressCode)) {
            throw exception(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED);
        }
    }

    private RentalDeviceDO requireBackfillReplayDeviceMatches(
            RentalDeviceShipmentDO replay, XianyuOrderDispatchBackfillReqVO reqVO) {
        RentalDeviceDO replayDevice = deviceMapper.selectById(replay.getDeviceId());
        boolean sameDevice = reqVO.getDeviceId() != null
                ? Objects.equals(reqVO.getDeviceId(), replay.getDeviceId())
                : replayDevice != null && StringUtils.hasText(reqVO.getDeviceNo())
                && Objects.equals(reqVO.getDeviceNo().trim(), replayDevice.getDeviceNo());
        if (!sameDevice) {
            throw exception(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED);
        }
        return replayDevice;
    }

    private String backfillRequestHash(XianyuOrderDispatchBackfillReqVO reqVO, Long deviceId,
                                       String waybillNo, String expressCode, String expressName) {
        String auditPayload = reqVO.getChannelOrderId() + "|" + deviceId + "|"
                + waybillNo + "|" + expressCode + "|" + expressName + "|"
                + reqVO.getConsignTime() + "|" + reqVO.getReason().trim();
        return DigestUtils.md5DigestAsHex(auditPayload.getBytes(StandardCharsets.UTF_8));
    }

    private XianyuOrderShipRespVO replayResponse(RentalDeviceShipmentDO shipment) {
        return replayResponse(shipment, deviceMapper.selectById(shipment.getDeviceId()));
    }

    private XianyuOrderShipRespVO replayResponse(RentalDeviceShipmentDO shipment, RentalDeviceDO device) {
        RentalDeliveryResult tracking = shipment.getDeliveryId() == null
                ? null : deliveryService.getResult(shipment.getDeliveryId());
        return toShipResp(shipment, device, "DISPATCHED", tracking);
    }

    private XianyuOrderShipRespVO toShipResp(RentalDeviceShipmentDO shipment, RentalDeviceDO device,
                                              String assignmentStatus, RentalDeliveryResult tracking) {
        XianyuOrderShipRespVO resp = new XianyuOrderShipRespVO();
        resp.setShipmentId(shipment.getId());
        resp.setChannelOrderId(shipment.getChannelOrderId());
        resp.setAssignmentId(shipment.getAssignmentId());
        resp.setDeviceId(shipment.getDeviceId());
        resp.setDeviceNo(device == null ? null : device.getDeviceNo());
        resp.setMaskedWaybillNo(tracking == null
                ? waybillPrivacy.mask(shipment.getWaybillNo()) : tracking.maskedWaybillNo());
        resp.setExpressCode(shipment.getExpressCode());
        resp.setExpressName(shipment.getExpressName());
        resp.setRemoteCode(shipment.getShipResponseCode());
        resp.setRemoteMsg(shipment.getShipResponseMsg());
        resp.setAssignmentStatus(assignmentStatus);
        resp.setSource(shipment.getSource());
        resp.setDeliveryId(shipment.getDeliveryId());
        if (tracking == null) {
            resp.setTrackingReason("LEGACY_SHIPMENT_WITHOUT_DELIVERY");
        } else {
            resp.setDeliveryId(tracking.deliveryId());
            resp.setTrackingMappingStatus(tracking.mappingStatus());
            resp.setTrackingSubscribeStatus(tracking.subscribeStatus());
            resp.setTrackingQueryStatus(tracking.queryStatus());
            resp.setTrackingReason(tracking.reasonCode());
            resp.setTrackingPendingEvents(tracking.pendingEventTypes());
        }
        return resp;
    }

    private record BackfillDispatchResult(
            RentalDeviceAssignmentResult assignment,
            String assignmentStatus
    ) {
    }

}
