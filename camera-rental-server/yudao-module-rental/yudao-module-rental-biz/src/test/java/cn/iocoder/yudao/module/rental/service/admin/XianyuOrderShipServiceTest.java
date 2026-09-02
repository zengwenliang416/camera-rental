package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderDispatchBackfillReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceShipmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
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
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryCreateCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import cn.iocoder.yudao.module.rental.service.logistics.WaybillPrivacy;
import cn.iocoder.yudao.module.rental.service.configuration.RentalChannelProductRuleService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationResult;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalOrderPreparationPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_DISPATCH_BACKFILL_CONFLICT;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_WRITE_DISABLED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_DISPATCH_BACKFILL_ORDER_NOT_SHIPPED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_DEVICE_NOT_SHIPPABLE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_IDEMPOTENT_KEY_REUSED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_ORDER_NOT_CONVERTED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_ORDER_NOT_READY;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_ORDER_NOT_PENDING;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_PRODUCT_RULE_BIND_REQUIRED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_REMOTE_ERROR;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuOrderShipServiceTest {

    private final XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
    private final XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
    private final RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
    private final RentalDeviceModelMapper deviceModelMapper = mock(RentalDeviceModelMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
    private final RentalOrderMapper rentalOrderMapper = mock(RentalOrderMapper.class);
    private final RentalOrderItemMapper rentalOrderItemMapper = mock(RentalOrderItemMapper.class);
    private final RentalDeviceShipmentMapper shipmentMapper = mock(RentalDeviceShipmentMapper.class);
    private final RentalOrderPreparationPolicy preparationPolicy = mock(RentalOrderPreparationPolicy.class);
    private final RentalChannelProductRuleService productRuleService =
            mock(RentalChannelProductRuleService.class);
    private final RentalChannelOrderReconciliationService reconciliationService =
            mock(RentalChannelOrderReconciliationService.class);
    private final RentalDeviceAssignmentService assignmentService = mock(RentalDeviceAssignmentService.class);
    private final RentalDeviceOpsService deviceOpsService = mock(RentalDeviceOpsService.class);
    private final RentalDeliveryService deliveryService = mock(RentalDeliveryService.class);
    private final WaybillPrivacy waybillPrivacy = new WaybillPrivacy();
    private final XianyuWriteClient writeClient = mock(XianyuWriteClient.class);
    private final XianyuProperties properties = new XianyuProperties();
    private final XianyuRuntimeConfigService runtimeConfigService = mock(XianyuRuntimeConfigService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(9L);
        properties.setEnabled(true);
        properties.setAppKey("test-app");
        properties.setAppSecret("test-secret");
        when(runtimeConfigService.getCurrent()).thenReturn(properties);
        when(deliveryService.createOrReuse(any())).thenReturn(trackingResult(
                "READY", "PENDING", "PENDING", null,
                List.of("SUBSCRIBE", "INITIAL_QUERY")));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shipRejectsBeforeLocalMutationWhenWriteSwitchIsDisabled() {
        properties.setWriteEnabled(false);
        XianyuOrderShipService service = service();

        ServiceException ex = assertThrows(ServiceException.class, () -> service.ship(req()));

        assertEquals(XIANYU_WRITE_DISABLED.getCode(), ex.getCode());
        verify(orderMapper, never()).selectByIdForUpdate(any());
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(any(), any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void pendingSearchUsesOfficialWaitShipStatus() {
        when(orderMapper.selectPendingShipPage(any(), any(), any(), any()))
                .thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));
        XianyuPendingShipOrderPageReqVO reqVO = new XianyuPendingShipOrderPageReqVO();
        ArgumentCaptor<Collection<String>> statuses = ArgumentCaptor.forClass(Collection.class);

        service().searchPendingOrders(reqVO);

        verify(orderMapper).selectPendingShipPage(any(), any(), statuses.capture(), eq(reqVO));
        assertEquals(true, statuses.getValue().contains("12"));
        assertEquals(false, statuses.getValue().contains("22"));
    }

    @Test
    void pendingSearchReturnsCompleteCustomerAndSellerRemarkFields() {
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(10L)
                .shopId(20L)
                .externalOrderId("test-order-001")
                .orderStatus("12")
                .goodsTitle("测试设备租赁")
                .goodsQuantity(1)
                .payAmount(12000L)
                .buyerNick("测试买家")
                .receiverName("测试收货人")
                .receiverMobile("19900000000")
                .receiverAddress("测试省测试市测试区测试路1号")
                .sellerRemark("测试卖家备注")
                .xianyuItemId("1062409679830")
                .conversionStatus("CONVERTED")
                .rentalOrderId(30L)
                .preparationStatus("WAITING_MODEL")
                .preparationReasonCode("PRODUCT_RULE_NOT_CONFIGURED")
                .build();
        when(orderMapper.selectPendingShipPage(any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(order), 1L));
        XianyuPendingShipOrderPageReqVO reqVO = new XianyuPendingShipOrderPageReqVO();
        reqVO.setKeyword("19900000000");

        XianyuPendingShipOrderRespVO result = service().searchPendingOrders(reqVO).getList().get(0);

        assertEquals("测试收货人", result.getReceiverName());
        assertEquals("19900000000", result.getReceiverMobile());
        assertEquals("测试省测试市测试区测试路1号", result.getReceiverAddress());
        assertEquals("测试卖家备注", result.getSellerRemark());
        assertEquals("1062409679830", result.getXianyuItemId());
        assertEquals("WAITING_MODEL", result.getPreparationStatus());
        assertEquals("PRODUCT_RULE_NOT_CONFIGURED", result.getPreparationReasonCode());
    }

    @Test
    void completedOrderCannotBeShipped() {
        properties.setWriteEnabled(true);
        XianyuOrderDO completed = pendingOrder();
        completed.setOrderStatus("22");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(completed);

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_ORDER_NOT_PENDING.getCode(), ex.getCode());
        verify(shopMapper, never()).selectByTenantIdAndId(any(), any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillDispatchedOrderCreatesLocalEvidenceWithoutRemoteWrite() {
        XianyuOrderDO order = shippedOrder();
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);

        XianyuOrderShipRespVO resp = service().backfillDispatch(backfillReq());

        assertEquals("ADMIN_BACKFILL", resp.getSource());
        assertEquals("DISPATCHED", resp.getAssignmentStatus());
        assertEquals(99L, resp.getDeliveryId());
        verify(writeClient, never()).execute(any(), any());
        verify(runtimeConfigService, never()).getCurrent();
        ArgumentCaptor<RentalDeviceShipmentDO> shipmentCaptor =
                ArgumentCaptor.forClass(RentalDeviceShipmentDO.class);
        ArgumentCaptor<RentalDeliveryCreateCommand> deliveryCaptor =
                ArgumentCaptor.forClass(RentalDeliveryCreateCommand.class);
        verify(shipmentMapper).insert(shipmentCaptor.capture());
        verify(deliveryService).createOrReuse(deliveryCaptor.capture());
        assertEquals("ADMIN_BACKFILL", shipmentCaptor.getValue().getSource());
        assertEquals("已发货补录：订单已在闲鱼后台发货，补录实际设备",
                shipmentCaptor.getValue().getShipResponseMsg());
        assertNotNull(shipmentCaptor.getValue().getShipRequestHash());
        assertEquals("OUTBOUND", deliveryCaptor.getValue().direction().name());
        assertEquals("shipment-backfill:99b4b469ec7a865972b32631d1da4108",
                deliveryCaptor.getValue().sourceIdentifier());
        assertEquals(40L, deliveryCaptor.getValue().devices().get(0).deviceId());
        assertEquals(LocalDateTime.of(2026, 8, 23, 14, 30), order.getConsignTime());
        assertEquals(LocalDate.of(2026, 8, 23), order.getShipDate());
        verify(orderMapper).updateById(order);
    }

    @Test
    void backfillRejectsOrderThatIsStillPendingShipment() {
        XianyuOrderDO order = pendingOrder();
        order.setWaybillNo("SF5113560342626");
        order.setConsignTime(LocalDateTime.of(2026, 8, 23, 14, 30));
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_DISPATCH_BACKFILL_ORDER_NOT_SHIPPED.getCode(), ex.getCode());
        verify(shopMapper, never()).selectByTenantIdAndId(any(), any());
        verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillRejectsRefundedClosedOrCancelledOrderBeforeMutation() {
        for (String status : List.of("23", "24")) {
            XianyuOrderDO order = shippedOrder();
            order.setOrderStatus(status);
            when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);

            ServiceException ex = assertThrows(
                    ServiceException.class, () -> service().backfillDispatch(backfillReq()));

            assertEquals(XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED.getCode(), ex.getCode());
        }
        XianyuOrderDO refunded = shippedOrder();
        refunded.setRefundStatus(5);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(refunded);

        ServiceException refundedEx = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED.getCode(), refundedEx.getCode());
        XianyuOrderDO cancelled = shippedOrder();
        cancelled.setCancelTime(LocalDateTime.of(2026, 8, 23, 15, 0));
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(cancelled);

        ServiceException cancelledEx = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED.getCode(), cancelledEx.getCode());
        verify(shopMapper, never()).selectByTenantIdAndId(any(), any());
        verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
        verify(assignmentService, never()).assign(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
        verify(deliveryService, never()).createOrReuse(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillRejectsNonShippableDeviceBeforeAssignment() {
        RentalDeviceDO disabledDevice = shippableDevice();
        disabledDevice.setEnabled(false);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(shippedOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(disabledDevice);
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(convertedOrderItem());

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE.getCode(), ex.getCode());
        verify(assignmentService, never()).assign(any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void backfillRejectsCrossTenantShopBeforeDeviceLookup() {
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(shippedOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(null);

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_SHOP_NOT_EXISTS.getCode(), ex.getCode());
        verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
        verify(assignmentService, never()).assign(any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
        verify(deliveryService, never()).createOrReuse(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillRejectsFormalShipmentWithSameBusinessKey() {
        RentalDeviceShipmentDO existing = RentalDeviceShipmentDO.builder()
                .id(80L)
                .channelOrderId(10L)
                .deviceId(40L)
                .deliveryId(99L)
                .waybillNo("SF5113560342626")
                .expressCode("shunfeng")
                .source("ADMIN")
                .build();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(shippedOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        stubPreparedInternalOrder();
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(shipmentMapper.selectByBusinessKeyForUpdate(
                10L, "SF5113560342626", "shunfeng")).thenReturn(existing);

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_DISPATCH_BACKFILL_CONFLICT.getCode(), ex.getCode());
        verify(rentalOrderMapper).selectByIdForUpdate(30L);
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void backfillRejectsSameWaybillAlreadyBoundToDifferentDevice() {
        RentalDeviceShipmentDO existing = RentalDeviceShipmentDO.builder()
                .id(80L)
                .channelOrderId(10L)
                .deviceId(41L)
                .waybillNo("SF5113560342626")
                .expressCode("shunfeng")
                .source("ADMIN_BACKFILL")
                .build();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(shippedOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        stubPreparedInternalOrder();
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(shipmentMapper.selectByBusinessKeyForUpdate(
                10L, "SF5113560342626", "shunfeng")).thenReturn(existing);

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_DISPATCH_BACKFILL_CONFLICT.getCode(), ex.getCode());
        verify(rentalOrderMapper).selectByIdForUpdate(30L);
        verify(assignmentService, never()).assign(any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
        verify(deliveryService, never()).createOrReuse(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillRejectsUnpreparedOrderInsteadOfCreatingShipmentMapping() {
        XianyuOrderDO order = shippedOrder();
        order.setRentalOrderId(null);
        order.setConversionStatus("PENDING");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_SHIP_ORDER_NOT_CONVERTED.getCode(), ex.getCode());
        verify(assignmentService, never()).assign(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
        verify(deliveryService, never()).createOrReuse(any());
        verify(orderMapper, never()).updateById(order);
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillDeliveryFailurePropagatesInsideRollbackForExceptionTransaction() throws NoSuchMethodException {
        XianyuOrderDO order = shippedOrder();
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);
        when(deliveryService.createOrReuse(any())).thenThrow(
                new RentalLogisticsException("DELIVERY_WRITE_FAILED"));

        RentalLogisticsException ex = assertThrows(
                RentalLogisticsException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals("DELIVERY_WRITE_FAILED", ex.getCode());
        Transactional transactional = XianyuOrderShipService.class
                .getMethod("backfillDispatch", XianyuOrderDispatchBackfillReqVO.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
        verify(assignmentService).assign(any());
        verify(deviceOpsService).dispatch(any());
        verify(shipmentMapper).insert(any(RentalDeviceShipmentDO.class));
        verify(shipmentMapper, never()).updateById(any(RentalDeviceShipmentDO.class));
        verify(orderMapper, never()).updateById(order);
        verify(writeClient, never()).execute(any(), any());
        verify(runtimeConfigService, never()).getCurrent();
    }

    @Test
    void backfillShipmentPersistenceFailureStopsOrderUpdate() {
        XianyuOrderDO order = shippedOrder();
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);
        when(shipmentMapper.updateById(any(RentalDeviceShipmentDO.class)))
                .thenThrow(new IllegalStateException("SHIPMENT_UPDATE_FAILED"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals("SHIPMENT_UPDATE_FAILED", ex.getMessage());
        verify(deliveryService).createOrReuse(any());
        verify(orderMapper, never()).updateById(order);
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillRejectsIdempotencyKeyReusedForDifferentOrder() {
        RentalDeviceShipmentDO replay = RentalDeviceShipmentDO.builder()
                .channelOrderId(11L)
                .deviceId(40L)
                .idempotencyKey("ship-key")
                .waybillNo("SF5113560342626")
                .expressCode("shunfeng")
                .source("ADMIN_BACKFILL")
                .build();
        when(shipmentMapper.selectByIdempotencyKeyForUpdate("ship-key")).thenReturn(replay);

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED.getCode(), ex.getCode());
        verify(orderMapper, never()).selectByIdForUpdate(any());
        verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillReplaysMatchingIdempotentRequest() {
        RentalDeviceDO device = shippableDevice();
        String auditPayload = "10|40|SF5113560342626|shunfeng|顺丰速运|"
                + "2026-08-23T14:30|订单已在闲鱼后台发货，补录实际设备";
        RentalDeviceShipmentDO replay = RentalDeviceShipmentDO.builder()
                .id(80L)
                .channelOrderId(10L)
                .assignmentId(60L)
                .deviceId(40L)
                .idempotencyKey("ship-key")
                .waybillNo("SF5113560342626")
                .expressCode("shunfeng")
                .expressName("顺丰速运")
                .shipRequestHash(DigestUtils.md5DigestAsHex(
                        auditPayload.getBytes(StandardCharsets.UTF_8)))
                .source("ADMIN_BACKFILL")
                .build();
        when(shipmentMapper.selectByIdempotencyKeyForUpdate("ship-key")).thenReturn(replay);
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(device);
        when(deviceMapper.selectById(40L)).thenReturn(device);

        XianyuOrderShipRespVO resp = service().backfillDispatch(backfillReq());

        assertEquals(80L, resp.getShipmentId());
        assertEquals("P4P-01-2JCW", resp.getDeviceNo());
        assertEquals("DISPATCHED", resp.getAssignmentStatus());
        verify(orderMapper, never()).selectByIdForUpdate(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void backfillRejectsChangedIdempotentRequestBeforeReplacementDeviceLookup() {
        XianyuOrderDispatchBackfillReqVO req = backfillReq();
        req.setDeviceNo("UNKNOWN-DEVICE");
        RentalDeviceShipmentDO replay = RentalDeviceShipmentDO.builder()
                .id(80L)
                .channelOrderId(10L)
                .assignmentId(60L)
                .deviceId(40L)
                .idempotencyKey("ship-key")
                .waybillNo("SF5113560342626")
                .expressCode("shunfeng")
                .source("ADMIN_BACKFILL")
                .build();
        when(shipmentMapper.selectByIdempotencyKeyForUpdate("ship-key")).thenReturn(replay);
        when(deviceMapper.selectById(40L)).thenReturn(shippableDevice());

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(req));

        assertEquals(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED.getCode(), ex.getCode());
        verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
        verify(orderMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void backfillRejectsChangedReasonForExistingIdempotencyKey() {
        XianyuOrderDispatchBackfillReqVO req = backfillReq();
        req.setReason("修改后的补录原因");
        RentalDeviceShipmentDO replay = RentalDeviceShipmentDO.builder()
                .id(80L)
                .channelOrderId(10L)
                .assignmentId(60L)
                .deviceId(40L)
                .idempotencyKey("ship-key")
                .waybillNo("SF5113560342626")
                .expressCode("shunfeng")
                .shipRequestHash("original-request-hash")
                .source("ADMIN_BACKFILL")
                .build();
        when(shipmentMapper.selectByIdempotencyKeyForUpdate("ship-key")).thenReturn(replay);
        when(deviceMapper.selectById(40L)).thenReturn(shippableDevice());

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(req));

        assertEquals(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED.getCode(), ex.getCode());
        verify(orderMapper, never()).selectByIdForUpdate(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void backfillMapsAssignmentScheduleConflictToBackfillConflict() {
        RentalOrderItemDO item = convertedOrderItem();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(shippedOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenThrow(new RentalDeviceAssignmentException(
                RentalDeviceAssignmentException.Code.SCHEDULE_CONFLICT, "conflict"));

        ServiceException ex = assertThrows(
                ServiceException.class, () -> service().backfillDispatch(backfillReq()));

        assertEquals(XIANYU_DISPATCH_BACKFILL_CONFLICT.getCode(), ex.getCode());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void backfillDispatchesExistingAssignedDevice() {
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceAssignmentDO existing = RentalDeviceAssignmentDO.builder()
                .id(60L)
                .scheduleId(70L)
                .rentalOrderId(30L)
                .rentalOrderItemId(50L)
                .deviceId(40L)
                .status("ASSIGNED")
                .build();
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(shippedOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentMapper.selectActiveByOrderItemAndDeviceForUpdate(50L, 40L)).thenReturn(existing);
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);

        XianyuOrderShipRespVO resp = service().backfillDispatch(backfillReq());

        assertEquals("DISPATCHED", resp.getAssignmentStatus());
        verify(assignmentService, never()).assign(any());
        verify(deviceOpsService).dispatch(any());
        verify(shipmentMapper).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void backfillReusesExistingDispatchedAssignmentWithoutDispatchingAgain() {
        XianyuOrderDO order = shippedOrder();
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceDO rentedDevice = shippableDevice();
        rentedDevice.setStatus("RENTED");
        RentalDeviceAssignmentDO existing = RentalDeviceAssignmentDO.builder()
                .id(60L)
                .scheduleId(70L)
                .rentalOrderId(30L)
                .rentalOrderItemId(50L)
                .deviceId(40L)
                .status("DISPATCHED")
                .build();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(rentedDevice);
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentMapper.selectActiveByOrderItemAndDeviceForUpdate(50L, 40L)).thenReturn(existing);

        XianyuOrderShipRespVO resp = service().backfillDispatch(backfillReq());

        assertEquals("DISPATCHED", resp.getAssignmentStatus());
        verify(assignmentService, never()).assign(any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper).insert(any(RentalDeviceShipmentDO.class));
        verify(deliveryService).createOrReuse(any());
    }

    @Test
    void shipCommitsLocalShipmentOnlyAfterRemoteSuccess() {
        properties.setWriteEnabled(true);
        XianyuOrderShipService service = service();
        XianyuOrderShipReqVO req = req();
        XianyuOrderDO order = pendingOrder();
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(writeClient.execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any()))
                .thenReturn(remoteSuccess());
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);

        XianyuOrderShipRespVO resp = service.ship(req);

        assertEquals(10L, resp.getChannelOrderId());
        assertEquals("P4P-01-2JCW", resp.getDeviceNo());
        assertEquals("DISPATCHED", resp.getAssignmentStatus());
        assertEquals(99L, resp.getDeliveryId());
        assertEquals(List.of("SUBSCRIBE", "INITIAL_QUERY"), resp.getTrackingPendingEvents());
        ArgumentCaptor<ObjectNode> shipBodyCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        ArgumentCaptor<RentalDeliveryCreateCommand> deliveryCaptor =
                ArgumentCaptor.forClass(RentalDeliveryCreateCommand.class);
        InOrder orderVerifier = inOrder(writeClient, deviceOpsService, shipmentMapper, deliveryService, orderMapper);
        orderVerifier.verify(writeClient).execute(eq(XianyuWriteEndpoint.ORDER_SHIP), shipBodyCaptor.capture());
        orderVerifier.verify(deviceOpsService).dispatch(any());
        ArgumentCaptor<RentalDeviceShipmentDO> shipmentCaptor =
                ArgumentCaptor.forClass(RentalDeviceShipmentDO.class);
        orderVerifier.verify(shipmentMapper).insert(shipmentCaptor.capture());
        orderVerifier.verify(deliveryService).createOrReuse(deliveryCaptor.capture());
        orderVerifier.verify(shipmentMapper).updateById(shipmentCaptor.getValue());
        orderVerifier.verify(orderMapper).updateById(order);
        assertEquals(99L, shipmentCaptor.getValue().getDeliveryId());
        assertEquals(30L, deliveryCaptor.getValue().rentalOrderId());
        assertEquals("OUTBOUND", deliveryCaptor.getValue().direction().name());
        assertEquals("XIANYU", deliveryCaptor.getValue().sourceType());
        assertEquals("shipment:99b4b469ec7a865972b32631d1da4108",
                deliveryCaptor.getValue().sourceIdentifier());
        assertEquals("shunfeng", deliveryCaptor.getValue().sourceCarrierCode());
        assertEquals("顺丰速运", deliveryCaptor.getValue().sourceCarrierName());
        assertEquals("SF5113560342626", deliveryCaptor.getValue().waybillNo());
        assertEquals("13800000000", deliveryCaptor.getValue().trackingPhone());
        assertEquals(50L, deliveryCaptor.getValue().devices().get(0).rentalOrderItemId());
        assertEquals(60L, deliveryCaptor.getValue().devices().get(0).assignmentId());
        assertEquals(40L, deliveryCaptor.getValue().devices().get(0).deviceId());
        assertEquals("SF5113560342626", shipmentCaptor.getValue().getWaybillNo());
        assertEquals("shunfeng", shipmentCaptor.getValue().getExpressCode());
        assertEquals("顺丰速运", shipmentCaptor.getValue().getExpressName());
        assertEquals("ADMIN", shipmentCaptor.getValue().getSource());
        assertEquals(Boolean.TRUE, shipmentCaptor.getValue().getOcrConfirmed());
        assertNotNull(shipmentCaptor.getValue().getShipRequestHash());
        assertEquals("3364202298717566229", shipBodyCaptor.getValue().path("order_no").asText());
        assertEquals("SF5113560342626", shipBodyCaptor.getValue().path("waybill_no").asText());
        assertEquals("shunfeng", shipBodyCaptor.getValue().path("express_code").asText());
        assertEquals("顺丰速运", shipBodyCaptor.getValue().path("express_name").asText());
        assertEquals(4, shipBodyCaptor.getValue().size());
        verify(preparationPolicy).requireReady(any(RentalOrderDO.class), eq(item));
    }

    @Test
    void shipRejectsWhenAuthoritativePreparationPolicyRejectsTheOrder() {
        properties.setWriteEnabled(true);
        XianyuOrderDO order = pendingOrder();
        RentalOrderDO rentalOrder = RentalOrderDO.builder()
                .id(30L)
                .preparationStatus("WAITING_REMARK")
                .preparationReasonCode("MISSING_REMARK")
                .build();
        RentalOrderItemDO item = convertedOrderItem();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder);
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        doThrow(new RentalDeviceAssignmentException(
                RentalDeviceAssignmentException.Code.ORDER_NOT_READY, "MISSING_REMARK"))
                .when(preparationPolicy).requireReady(rentalOrder, item);

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_ORDER_NOT_READY.getCode(), ex.getCode());
        verify(preparationPolicy).requireReady(rentalOrder, item);
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void shipRequiresExplicitConfirmationBeforeCreatingMissingProductRule() {
        properties.setWriteEnabled(true);
        XianyuOrderDO order = pendingOrder();
        order.setXianyuItemId("1062409679830");
        RentalOrderDO rentalOrder = RentalOrderDO.builder()
                .id(30L)
                .preparationStatus("WAITING_MODEL")
                .preparationReasonCode("PRODUCT_RULE_NOT_CONFIGURED")
                .build();
        RentalOrderItemDO item = convertedOrderItem();
        item.setEquipmentModelCode(null);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder);
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        doThrow(new RentalDeviceAssignmentException(
                RentalDeviceAssignmentException.Code.ORDER_NOT_READY, "PRODUCT_RULE_NOT_CONFIGURED"))
                .when(preparationPolicy).requireReady(rentalOrder, item);

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_PRODUCT_RULE_BIND_REQUIRED.getCode(), ex.getCode());
        verify(productRuleService, never()).createSingleRuleFromShipment(any(), any(), any());
        verify(reconciliationService, never()).reconcile(any());
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void shipCreatesMissingSingleModelRuleAndReconcilesBeforeDispatch() {
        properties.setWriteEnabled(true);
        XianyuOrderShipReqVO req = req();
        req.setBindProductRuleIfMissing(true);
        XianyuOrderDO order = pendingOrder();
        order.setXianyuItemId("1062409679830");
        RentalOrderDO waitingOrder = RentalOrderDO.builder()
                .id(30L)
                .preparationStatus("WAITING_MODEL")
                .preparationReasonCode("PRODUCT_RULE_NOT_CONFIGURED")
                .build();
        RentalOrderDO readyOrder = RentalOrderDO.builder()
                .id(30L)
                .preparationStatus("READY")
                .build();
        RentalOrderItemDO waitingItem = convertedOrderItem();
        waitingItem.setEquipmentModelCode(null);
        RentalOrderItemDO readyItem = convertedOrderItem();
        RentalDeviceDO device = shippableDevice();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(device);
        when(deviceModelMapper.selectByCode("DJI-P4P")).thenReturn(RentalDeviceModelDO.builder()
                .id(300L)
                .modelCode("DJI-P4P")
                .enabled(true)
                .build());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(waitingOrder, readyOrder);
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L))
                .thenReturn(waitingItem, readyItem);
        doThrow(new RentalDeviceAssignmentException(
                RentalDeviceAssignmentException.Code.ORDER_NOT_READY, "PRODUCT_RULE_NOT_CONFIGURED"))
                .doNothing()
                .when(preparationPolicy).requireReady(any(RentalOrderDO.class), any(RentalOrderItemDO.class));
        when(reconciliationService.reconcile(10L)).thenReturn(
                new RentalChannelOrderReconciliationResult(
                        "CONVERTED", 30L, null, null, "READY", true, "UPDATED"));
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, readyItem.getOccupyStartDate(), readyItem.getOccupyEndDateExclusive()));
        when(writeClient.execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any())).thenReturn(remoteSuccess());
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);

        XianyuOrderShipRespVO response = service().ship(req);

        assertEquals("DISPATCHED", response.getAssignmentStatus());
        InOrder bindingOrder = inOrder(productRuleService, reconciliationService, assignmentService, writeClient);
        bindingOrder.verify(productRuleService)
                .createSingleRuleFromShipment(20L, "1062409679830", 300L);
        bindingOrder.verify(reconciliationService).reconcile(10L);
        bindingOrder.verify(assignmentService).assign(any());
        bindingOrder.verify(writeClient).execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any());
    }

    @Test
    void shipRejectsUnpreparedOrderInsteadOfUsingSelectedDeviceModel() {
        properties.setWriteEnabled(true);
        XianyuOrderDO order = pendingOrder();
        order.setRentalOrderId(null);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_ORDER_NOT_CONVERTED.getCode(), ex.getCode());
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any());
    }

    @Test
    void shipReusesDeviceAlreadyAssignedToTheSameOrderItem() {
        properties.setWriteEnabled(true);
        XianyuOrderShipReqVO req = req();
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceAssignmentDO existing = RentalDeviceAssignmentDO.builder()
                .id(60L)
                .scheduleId(70L)
                .rentalOrderId(30L)
                .rentalOrderItemId(50L)
                .deviceId(40L)
                .status("ASSIGNED")
                .build();
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentMapper.selectActiveByOrderItemAndDeviceForUpdate(50L, 40L)).thenReturn(existing);
        when(writeClient.execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any())).thenReturn(remoteSuccess());
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);

        XianyuOrderShipRespVO resp = service().ship(req);

        assertEquals(60L, resp.getAssignmentId());
        verify(assignmentService, never()).assign(any());
        verify(deviceOpsService).dispatch(any());
        verify(shipmentMapper).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void shipRejectsUnauthorizedShopBeforeDeviceAssignmentOrRemoteWrite() {
        properties.setWriteEnabled(true);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(XianyuShopDO.builder()
                .id(20L)
                .authorizationStatus("INVALID")
                .authorizationExpiresAt(LocalDateTime.now().plusDays(1))
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHOP_AUTHORIZATION_INVALID.getCode(), ex.getCode());
        verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(any(), any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void shipRejectsNonShippableDeviceBeforeAssignmentOrRemoteWrite() {
        properties.setWriteEnabled(true);
        RentalDeviceDO disabledDevice = shippableDevice();
        disabledDevice.setEnabled(false);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        stubPreparedInternalOrder();
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(disabledDevice);

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE.getCode(), ex.getCode());
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(any(), any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void shipResolvesScannedSerialNumberAfterDeviceNoMiss() {
        properties.setWriteEnabled(true);
        XianyuOrderShipReqVO req = req();
        req.setDeviceNo("SERIAL-P4P-001");
        RentalDeviceDO disabledDevice = shippableDevice();
        disabledDevice.setSerialNumber("SERIAL-P4P-001");
        disabledDevice.setEnabled(false);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        stubPreparedInternalOrder();
        when(deviceMapper.selectBySerialNumberForUpdate("SERIAL-P4P-001")).thenReturn(disabledDevice);

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req));

        assertEquals(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE.getCode(), ex.getCode());
        InOrder lookupOrder = inOrder(deviceMapper);
        lookupOrder.verify(deviceMapper).selectByDeviceNoForUpdate("SERIAL-P4P-001");
        lookupOrder.verify(deviceMapper).selectBySerialNumberForUpdate("SERIAL-P4P-001");
        verify(deviceMapper, never()).selectByLegacyDeviceNoForUpdate(any());
    }

    @Test
    void backfillResolvesScannedLegacyDeviceNoAfterCurrentIdentifiersMiss() {
        XianyuOrderDispatchBackfillReqVO req = backfillReq();
        req.setDeviceNo("LEGACY-P4P-001");
        RentalDeviceDO disabledDevice = shippableDevice();
        disabledDevice.setLegacyDeviceNo("LEGACY-P4P-001");
        disabledDevice.setEnabled(false);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(shippedOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByLegacyDeviceNoForUpdate("LEGACY-P4P-001")).thenReturn(disabledDevice);
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(convertedOrderItem());

        ServiceException ex = assertThrows(ServiceException.class, () -> service().backfillDispatch(req));

        assertEquals(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE.getCode(), ex.getCode());
        InOrder lookupOrder = inOrder(deviceMapper);
        lookupOrder.verify(deviceMapper).selectByDeviceNoForUpdate("LEGACY-P4P-001");
        lookupOrder.verify(deviceMapper).selectBySerialNumberForUpdate("LEGACY-P4P-001");
        lookupOrder.verify(deviceMapper).selectByLegacyDeviceNoForUpdate("LEGACY-P4P-001");
    }

    @Test
    void shipRejectsUnknownScannedDeviceIdentifier() {
        properties.setWriteEnabled(true);
        XianyuOrderShipReqVO req = req();
        req.setDeviceNo("UNKNOWN-DEVICE");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        stubPreparedInternalOrder();

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req));

        assertEquals(RENTAL_DEVICE_NOT_EXISTS.getCode(), ex.getCode());
        InOrder lookupOrder = inOrder(deviceMapper);
        lookupOrder.verify(deviceMapper).selectByDeviceNoForUpdate("UNKNOWN-DEVICE");
        lookupOrder.verify(deviceMapper).selectBySerialNumberForUpdate("UNKNOWN-DEVICE");
        lookupOrder.verify(deviceMapper).selectByLegacyDeviceNoForUpdate("UNKNOWN-DEVICE");
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(any(), any());
    }

    @Test
    void shipPrefersDeviceIdWhenBothDeviceIdAndScannedValueAreProvided() {
        properties.setWriteEnabled(true);
        XianyuOrderShipReqVO req = req();
        req.setDeviceId(40L);
        req.setDeviceNo("SERIAL-IGNORED");
        RentalDeviceDO disabledDevice = shippableDevice();
        disabledDevice.setEnabled(false);
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        stubPreparedInternalOrder();
        when(deviceMapper.selectByIdForUpdate(40L)).thenReturn(disabledDevice);

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req));

        assertEquals(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE.getCode(), ex.getCode());
        verify(deviceMapper).selectByIdForUpdate(40L);
        verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
        verify(deviceMapper, never()).selectBySerialNumberForUpdate(any());
        verify(deviceMapper, never()).selectByLegacyDeviceNoForUpdate(any());
    }

    @Test
    void remoteFailureDoesNotDispatchInsertShipmentOrUpdateOrder() {
        properties.setWriteEnabled(true);
        RentalOrderItemDO item = convertedOrderItem();
        XianyuOrderDO order = pendingOrder();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(writeClient.execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any()))
                .thenThrow(new XianyuClientException(XianyuClientException.Kind.TRANSPORT, "timeout"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_REMOTE_ERROR.getCode(), ex.getCode());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
        verify(orderMapper, never()).updateById(any(XianyuOrderDO.class));
    }

    @Test
    void idempotentReplayReturnsDeviceNo() {
        properties.setWriteEnabled(false);
        when(shipmentMapper.selectByIdempotencyKeyForUpdate("ship-key")).thenReturn(RentalDeviceShipmentDO.builder()
                .id(90L)
                .channelOrderId(10L)
                .assignmentId(60L)
                .deviceId(40L)
                .waybillNo("SF5113560342626")
                .expressCode("shunfeng")
                .expressName("顺丰速运")
                .source("ADMIN")
                .build());
        when(deviceMapper.selectById(40L)).thenReturn(RentalDeviceDO.builder()
                .id(40L)
                .deviceNo("P4P-01-2JCW")
                .build());

        XianyuOrderShipRespVO resp = service().ship(req());

        assertEquals("P4P-01-2JCW", resp.getDeviceNo());
        assertEquals("SF5****2626", resp.getMaskedWaybillNo());
        assertEquals("LEGACY_SHIPMENT_WITHOUT_DELIVERY", resp.getTrackingReason());
        verify(writeClient, never()).execute(any(), any());
        verify(deliveryService, never()).createOrReuse(any());
        verify(deliveryService, never()).getResult(any());
    }

    @Test
    void idempotentReplayReturnsLinkedDeliveryWithoutDuplicatingTasks() {
        properties.setWriteEnabled(false);
        when(shipmentMapper.selectByIdempotencyKeyForUpdate("ship-key")).thenReturn(
                RentalDeviceShipmentDO.builder()
                        .id(90L)
                        .channelOrderId(10L)
                        .assignmentId(60L)
                        .deviceId(40L)
                        .deliveryId(99L)
                        .waybillNo("SF5113560342626")
                        .expressCode("shunfeng")
                        .expressName("顺丰速运")
                        .source("ADMIN")
                        .build());
        when(deviceMapper.selectById(40L)).thenReturn(
                RentalDeviceDO.builder().id(40L).deviceNo("P4P-01-2JCW").build());
        when(deliveryService.getResult(99L)).thenReturn(trackingResult(
                "READY", "SUBSCRIBED", "READY", null, List.of(), "DELIVERY_MASKED"));

        XianyuOrderShipRespVO resp = service().ship(req());

        assertEquals(99L, resp.getDeliveryId());
        assertEquals("DELIVERY_MASKED", resp.getMaskedWaybillNo());
        assertEquals("SUBSCRIBED", resp.getTrackingSubscribeStatus());
        assertEquals(List.of(), resp.getTrackingPendingEvents());
        verify(deliveryService).getResult(99L);
        verify(deliveryService, never()).createOrReuse(any());
        verify(writeClient, never()).execute(any(), any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void mappingOrProviderDegradationDoesNotChangeShipmentSuccess() {
        properties.setWriteEnabled(true);
        stubSuccessfulShipment();
        when(deliveryService.createOrReuse(any())).thenReturn(trackingResult(
                "MAPPING_REQUIRED", "MAPPING_REQUIRED", "MAPPING_REQUIRED", "MAPPING_REQUIRED",
                List.of("SUBSCRIBE", "INITIAL_QUERY")));

        XianyuOrderShipRespVO mappingRequired = service().ship(req());

        assertEquals(99L, mappingRequired.getDeliveryId());
        assertEquals("MAPPING_REQUIRED", mappingRequired.getTrackingReason());
        assertEquals("MAPPING_REQUIRED", mappingRequired.getTrackingMappingStatus());
    }

    @Test
    void providerDisabledDoesNotChangeShipmentSuccess() {
        properties.setWriteEnabled(true);
        stubSuccessfulShipment();
        when(deliveryService.createOrReuse(any())).thenReturn(trackingResult(
                "READY", "PROVIDER_DISABLED", "PROVIDER_DISABLED", "PROVIDER_DISABLED",
                List.of("SUBSCRIBE", "INITIAL_QUERY")));

        XianyuOrderShipRespVO providerDisabled = service().ship(req());

        assertEquals(99L, providerDisabled.getDeliveryId());
        assertEquals("PROVIDER_DISABLED", providerDisabled.getTrackingReason());
        assertEquals("PROVIDER_DISABLED", providerDisabled.getTrackingSubscribeStatus());
    }

    @Test
    void deliveryFailureStopsRemainingLocalWritesAfterRemoteSuccess() {
        properties.setWriteEnabled(true);
        XianyuOrderDO order = stubSuccessfulShipment();
        when(deliveryService.createOrReuse(any())).thenThrow(new RentalLogisticsException("DELIVERY_WRITE_FAILED"));

        RentalLogisticsException exception =
                assertThrows(RentalLogisticsException.class, () -> service().ship(req()));

        assertEquals("DELIVERY_WRITE_FAILED", exception.getCode());
        verify(writeClient).execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any());
        verify(deviceOpsService).dispatch(any());
        verify(shipmentMapper).insert(any(RentalDeviceShipmentDO.class));
        verify(shipmentMapper, never()).updateById(any(RentalDeviceShipmentDO.class));
        verify(orderMapper, never()).updateById(order);
    }

    @Test
    void staffSourceUsesSameBackendPathAndPersistsSource() {
        properties.setWriteEnabled(true);
        XianyuOrderShipReqVO req = req();
        req.setSource("STAFF");
        XianyuOrderDO order = pendingOrder();
        RentalOrderItemDO item = convertedOrderItem();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(writeClient.execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any()))
                .thenReturn(remoteSuccess());
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);

        XianyuOrderShipRespVO resp = service().ship(req);

        assertEquals("STAFF", resp.getSource());
        ArgumentCaptor<RentalDeviceShipmentDO> shipmentCaptor =
                ArgumentCaptor.forClass(RentalDeviceShipmentDO.class);
        verify(shipmentMapper).insert(shipmentCaptor.capture());
        assertEquals("STAFF", shipmentCaptor.getValue().getSource());
        verify(writeClient).execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any());
    }

    @Test
    void assignmentIdempotencyConflictRejectsBeforeRemoteWrite() {
        properties.setWriteEnabled(true);
        RentalOrderItemDO item = convertedOrderItem();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenThrow(new RentalDeviceAssignmentException(
                RentalDeviceAssignmentException.Code.IDEMPOTENCY_KEY_REUSED, "reused"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_IDEMPOTENT_KEY_REUSED.getCode(), ex.getCode());
        verify(writeClient, never()).execute(any(), any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
    }

    @Test
    void crossTenantShopReferenceRejectsAdminAndStaffBeforeRemoteWrite() {
        for (String source : new String[]{"ADMIN", "STAFF"}) {
            properties.setWriteEnabled(true);
            XianyuOrderShipReqVO req = req();
            req.setSource(source);
            when(orderMapper.selectByIdForUpdate(10L)).thenReturn(pendingOrder());
            when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req));

            assertEquals(XIANYU_SHOP_NOT_EXISTS.getCode(), ex.getCode());
            verify(deviceMapper, never()).selectByDeviceNoForUpdate(any());
            verify(assignmentService, never()).assign(any());
            verify(writeClient, never()).execute(any(), any());
            verify(deviceOpsService, never()).dispatch(any());
            verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
        }
    }

    private XianyuOrderShipService service() {
        return new XianyuOrderShipService(orderMapper, shopMapper, deviceMapper, deviceModelMapper,
                assignmentMapper, rentalOrderMapper, rentalOrderItemMapper, shipmentMapper, preparationPolicy,
                productRuleService, reconciliationService, assignmentService, deviceOpsService,
                deliveryService, waybillPrivacy, writeClient, runtimeConfigService, objectMapper);
    }

    private XianyuOrderDO stubSuccessfulShipment() {
        XianyuOrderDO order = pendingOrder();
        RentalOrderItemDO item = convertedOrderItem();
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(writeClient.execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any())).thenReturn(remoteSuccess());
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);
        return order;
    }

    private RentalDeliveryResult trackingResult(String mappingStatus, String subscribeStatus, String queryStatus,
                                                String reasonCode, List<String> pendingEventTypes) {
        return trackingResult(mappingStatus, subscribeStatus, queryStatus, reasonCode, pendingEventTypes,
                "SF****2626");
    }

    private RentalDeliveryResult trackingResult(String mappingStatus, String subscribeStatus, String queryStatus,
                                                String reasonCode, List<String> pendingEventTypes,
                                                String maskedWaybillNo) {
        return new RentalDeliveryResult(99L, true, mappingStatus, subscribeStatus, queryStatus,
                maskedWaybillNo, reasonCode, pendingEventTypes);
    }

    private XianyuOrderShipReqVO req() {
        XianyuOrderShipReqVO req = new XianyuOrderShipReqVO();
        req.setChannelOrderId(10L);
        req.setDeviceNo("P4P-01-2JCW");
        req.setIdempotencyKey("ship-key");
        req.setExpressCode("shunfeng");
        req.setExpressName("顺丰速运");
        req.setWaybillNo("SF5113560342626");
        req.setSource("ADMIN");
        req.setOcrConfirmed(true);
        return req;
    }

    private XianyuOrderDispatchBackfillReqVO backfillReq() {
        XianyuOrderDispatchBackfillReqVO req = new XianyuOrderDispatchBackfillReqVO();
        req.setChannelOrderId(10L);
        req.setDeviceNo("P4P-01-2JCW");
        req.setIdempotencyKey("ship-key");
        req.setExpressCode("shunfeng");
        req.setExpressName("顺丰速运");
        req.setWaybillNo("SF5113560342626");
        req.setConsignTime(LocalDateTime.of(2026, 8, 23, 14, 30));
        req.setReason("订单已在闲鱼后台发货，补录实际设备");
        return req;
    }

    private XianyuOrderDO pendingOrder() {
        return XianyuOrderDO.builder()
                .id(10L)
                .shopId(20L)
                .externalOrderId("3364202298717566229")
                .orderStatus("12")
                .rentalOrderId(30L)
                .receiverMobile("13800000000")
                .build();
    }

    private XianyuOrderDO shippedOrder() {
        XianyuOrderDO order = pendingOrder();
        order.setOrderStatus("21");
        order.setWaybillNo("SF5113560342626");
        order.setExpressCode("shunfeng");
        order.setExpressName("顺丰速运");
        return order;
    }

    private XianyuShopDO validShop() {
        return XianyuShopDO.builder()
                .id(20L)
                .authorizationStatus("VALID")
                .authorizationExpiresAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    private RentalDeviceDO shippableDevice() {
        return RentalDeviceDO.builder()
                .id(40L)
                .deviceNo("P4P-01-2JCW")
                .equipmentModelCode("DJI-P4P")
                .enabled(true)
                .status("AVAILABLE")
                .build();
    }

    private RentalOrderItemDO convertedOrderItem() {
        return RentalOrderItemDO.builder()
                .id(50L)
                .rentalOrderId(30L)
                .equipmentModelCode("DJI-P4P")
                .occupyStartDate(LocalDate.of(2026, 7, 27))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 2))
                .build();
    }

    private void stubPreparedInternalOrder() {
        when(rentalOrderMapper.selectByIdForUpdate(30L))
                .thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L))
                .thenReturn(convertedOrderItem());
    }

    private XianyuReadResponse remoteSuccess() {
        ObjectNode remotePayload = objectMapper.createObjectNode();
        remotePayload.put("code", 0);
        remotePayload.put("msg", "ok");
        return new XianyuReadResponse(200, 0, remotePayload, remotePayload.toString());
    }

}
