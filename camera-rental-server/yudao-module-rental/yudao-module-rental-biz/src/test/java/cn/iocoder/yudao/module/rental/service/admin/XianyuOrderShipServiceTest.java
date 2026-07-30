package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipRespVO;
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
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_WRITE_DISABLED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_DEVICE_NOT_SHIPPABLE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_IDEMPOTENT_KEY_REUSED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_ORDER_NOT_PENDING;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_REMOTE_ERROR;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuOrderShipServiceTest {

    private final XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
    private final XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
    private final RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
    private final RentalOrderMapper rentalOrderMapper = mock(RentalOrderMapper.class);
    private final RentalOrderItemMapper rentalOrderItemMapper = mock(RentalOrderItemMapper.class);
    private final RentalDeviceShipmentMapper shipmentMapper = mock(RentalDeviceShipmentMapper.class);
    private final XianyuRentalConversionService conversionService = mock(XianyuRentalConversionService.class);
    private final RentalDeviceAssignmentService assignmentService = mock(RentalDeviceAssignmentService.class);
    private final RentalDeviceOpsService deviceOpsService = mock(RentalDeviceOpsService.class);
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
                .conversionStatus("CONVERTED")
                .rentalOrderId(30L)
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
        ArgumentCaptor<ObjectNode> shipBodyCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        InOrder orderVerifier = inOrder(writeClient, deviceOpsService, shipmentMapper, orderMapper);
        orderVerifier.verify(writeClient).execute(eq(XianyuWriteEndpoint.ORDER_SHIP), shipBodyCaptor.capture());
        orderVerifier.verify(deviceOpsService).dispatch(any());
        ArgumentCaptor<RentalDeviceShipmentDO> shipmentCaptor =
                ArgumentCaptor.forClass(RentalDeviceShipmentDO.class);
        orderVerifier.verify(shipmentMapper).insert(shipmentCaptor.capture());
        orderVerifier.verify(orderMapper).updateById(order);
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
    }

    @Test
    void shipConvertsUnmappedOrderUsingSelectedDeviceModelBeforeAssignment() {
        properties.setWriteEnabled(true);
        XianyuOrderDO order = pendingOrder();
        order.setRentalOrderId(null);
        RentalOrderItemDO item = convertedOrderItem();
        RentalDeviceOpsRespVO dispatched = new RentalDeviceOpsRespVO();
        dispatched.setAssignmentStatus("DISPATCHED");
        when(orderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(shippableDevice());
        when(conversionService.convertForShipment(10L, "DJI-P4P"))
                .thenReturn(RentalConversionResult.converted(30L));
        when(rentalOrderMapper.selectByIdForUpdate(30L)).thenReturn(RentalOrderDO.builder().id(30L).build());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(30L)).thenReturn(item);
        when(assignmentService.assign(any())).thenReturn(new RentalDeviceAssignmentResult(
                60L, 70L, 40L, item.getOccupyStartDate(), item.getOccupyEndDateExclusive()));
        when(writeClient.execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any())).thenReturn(remoteSuccess());
        when(deviceOpsService.dispatch(any())).thenReturn(dispatched);

        XianyuOrderShipRespVO resp = service().ship(req());

        assertEquals(60L, resp.getAssignmentId());
        verify(conversionService).convertForShipment(10L, "DJI-P4P");
        verify(assignmentService).assign(any());
        verify(writeClient).execute(eq(XianyuWriteEndpoint.ORDER_SHIP), any());
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
        when(deviceMapper.selectByDeviceNoForUpdate("P4P-01-2JCW")).thenReturn(disabledDevice);

        ServiceException ex = assertThrows(ServiceException.class, () -> service().ship(req()));

        assertEquals(XIANYU_SHIP_DEVICE_NOT_SHIPPABLE.getCode(), ex.getCode());
        verify(assignmentService, never()).assign(any());
        verify(writeClient, never()).execute(any(), any());
        verify(deviceOpsService, never()).dispatch(any());
        verify(shipmentMapper, never()).insert(any(RentalDeviceShipmentDO.class));
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
        assertEquals("SF****2626", resp.getMaskedWaybillNo());
        verify(writeClient, never()).execute(any(), any());
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
        return new XianyuOrderShipService(orderMapper, shopMapper, deviceMapper, assignmentMapper, rentalOrderMapper,
                rentalOrderItemMapper, shipmentMapper, conversionService, assignmentService, deviceOpsService, writeClient,
                runtimeConfigService, objectMapper);
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

    private XianyuOrderDO pendingOrder() {
        return XianyuOrderDO.builder()
                .id(10L)
                .shopId(20L)
                .externalOrderId("3364202298717566229")
                .orderStatus("12")
                .rentalOrderId(30L)
                .build();
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
                .occupyStartDate(LocalDate.of(2026, 7, 27))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 2))
                .build();
    }

    private XianyuReadResponse remoteSuccess() {
        ObjectNode remotePayload = objectMapper.createObjectNode();
        remotePayload.put("code", 0);
        remotePayload.put("msg", "ok");
        return new XianyuReadResponse(200, 0, remotePayload, remotePayload.toString());
    }

}
