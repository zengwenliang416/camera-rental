package cn.iocoder.yudao.module.rental.service.rental;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalCustomerDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalCustomerMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceOpsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_ASSIGNMENT_INCOMPLETE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_CONFIRM_EXPRESS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_DELIVERY_METHOD_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_DEVICE_ASSIGN_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_MODEL_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalManualOrderServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 6);

    private final RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
    private final RentalOrderItemMapper orderItemMapper = mock(RentalOrderItemMapper.class);
    private final RentalCustomerMapper customerMapper = mock(RentalCustomerMapper.class);
    private final RentalOrderDeliveryMapper orderDeliveryMapper = mock(RentalOrderDeliveryMapper.class);
    private final RentalDeviceModelMapper deviceModelMapper = mock(RentalDeviceModelMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
    private final RentalDeviceAssignmentService assignmentService = mock(RentalDeviceAssignmentService.class);
    private final RentalDeviceOpsService deviceOpsService = mock(RentalDeviceOpsService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-06T02:00:00Z"), BUSINESS_ZONE);
    private final RentalManualOrderServiceImpl service = new RentalManualOrderServiceImpl(
            orderMapper, orderItemMapper, customerMapper, orderDeliveryMapper, deviceModelMapper,
            assignmentMapper, assignmentService, deviceOpsService, clock);

    private RentalManualOrderCreateReqVO validReqVO() {
        RentalManualOrderCreateReqVO reqVO = new RentalManualOrderCreateReqVO();
        RentalManualOrderCreateReqVO.Customer customer = new RentalManualOrderCreateReqVO.Customer();
        customer.setName("张三");
        customer.setMobile("13800000001");
        customer.setWechatId("wx-zhangsan");
        reqVO.setCustomer(customer);
        RentalManualOrderCreateReqVO.Item item = new RentalManualOrderCreateReqVO.Item();
        item.setModelCode("P4P");
        item.setQuantity(2);
        item.setDeviceIds(List.of(101L, 102L));
        item.setRentAmount(30000L);
        reqVO.setItems(List.of(item));
        reqVO.setBillableStartDate(TODAY.plusDays(1));
        reqVO.setBillableEndDate(TODAY.plusDays(4));
        reqVO.setDepositAmount(500000L);
        RentalManualOrderCreateReqVO.Delivery delivery = new RentalManualOrderCreateReqVO.Delivery();
        delivery.setMethod("ERRAND");
        delivery.setReceiverName("李四");
        delivery.setReceiverMobile("13900000002");
        delivery.setReceiverAddress("上海市徐汇区某路 1 号");
        delivery.setRemark("下午送达");
        reqVO.setDelivery(delivery);
        return reqVO;
    }

    private void stubEnabledModel(String modelCode) {
        when(deviceModelMapper.selectByCode(modelCode)).thenReturn(RentalDeviceModelDO.builder()
                .id(11L).modelCode(modelCode).enabled(true).build());
        when(orderMapper.insert(any(RentalOrderDO.class))).thenAnswer(invocation -> {
            RentalOrderDO order = invocation.getArgument(0);
            order.setId(456L);
            return 1;
        });
        lenient().when(customerMapper.insert(any(RentalCustomerDO.class))).thenAnswer(invocation -> {
            RentalCustomerDO customer = invocation.getArgument(0);
            customer.setId(78L);
            return 1;
        });
        lenient().when(orderItemMapper.insert(any(RentalOrderItemDO.class))).thenAnswer(invocation -> {
            RentalOrderItemDO item = invocation.getArgument(0);
            item.setId(789L);
            return 1;
        });
    }

    @Test
    void createManualOrderPersistsOfflineReadyOrderWithBackfilledOrderNo() {
        stubEnabledModel("P4P");

        RentalManualOrderCreateRespVO respVO = service.createManualOrder(validReqVO());

        assertEquals(456L, respVO.getId());
        assertEquals("OFF-0000000000000000456", respVO.getOrderNo());

        ArgumentCaptor<RentalOrderDO> orderCaptor = ArgumentCaptor.forClass(RentalOrderDO.class);
        verify(orderMapper).insert(orderCaptor.capture());
        RentalOrderDO inserted = orderCaptor.getValue();
        assertEquals("OFFLINE", inserted.getSourceType());
        assertEquals("PENDING_ALLOCATION", inserted.getStatus());
        assertEquals("READY", inserted.getPreparationStatus());
        assertEquals(78L, inserted.getCustomerId());
        assertEquals(30000L, inserted.getRentAmount());
        assertEquals(500000L, inserted.getDepositAmount());
        assertEquals(0L, inserted.getRefundAmount());
        assertEquals(TODAY.plusDays(1), inserted.getBillableStartDate());
        assertEquals(TODAY.plusDays(4), inserted.getBillableEndDate());
        assertEquals(TODAY.plusDays(1), inserted.getOccupyStartDate());
        assertEquals(TODAY.plusDays(5), inserted.getOccupyEndDateExclusive());
        assertEquals(TODAY.plusDays(4), inserted.getExpectedSendBackDate());
        assertNull(inserted.getChannelOrderId());
        assertNull(inserted.getSourceOrderId());

        ArgumentCaptor<RentalOrderDO> updateCaptor = ArgumentCaptor.forClass(RentalOrderDO.class);
        verify(orderMapper).updateById(updateCaptor.capture());
        assertEquals("OFF-0000000000000000456", updateCaptor.getValue().getOrderNo());

        ArgumentCaptor<RentalOrderItemDO> itemCaptor = ArgumentCaptor.forClass(RentalOrderItemDO.class);
        verify(orderItemMapper).insert(itemCaptor.capture());
        RentalOrderItemDO item = itemCaptor.getValue();
        assertEquals(456L, item.getRentalOrderId());
        assertEquals("P4P", item.getEquipmentModelCode());
        assertEquals(2, item.getQuantity());
        assertEquals(30000L, item.getRentAmount());
        assertEquals(TODAY.plusDays(1), item.getOccupyStartDate());
        assertEquals(TODAY.plusDays(5), item.getOccupyEndDateExclusive());
        assertEquals(TODAY.plusDays(4), item.getExpectedSendBackDate());

        ArgumentCaptor<RentalOrderDeliveryDO> deliveryCaptor =
                ArgumentCaptor.forClass(RentalOrderDeliveryDO.class);
        verify(orderDeliveryMapper).insert(deliveryCaptor.capture());
        RentalOrderDeliveryDO delivery = deliveryCaptor.getValue();
        assertEquals(456L, delivery.getRentalOrderId());
        assertEquals("ERRAND", delivery.getDeliveryMethod());
        assertEquals("李四", delivery.getReceiverName());
        assertEquals("13900000002", delivery.getReceiverMobile());
        assertEquals("上海市徐汇区某路 1 号", delivery.getReceiverAddress());
        assertEquals("下午送达", delivery.getDeliveryRemark());

        ArgumentCaptor<RentalDeviceAssignmentCommand> assignmentCaptor =
                ArgumentCaptor.forClass(RentalDeviceAssignmentCommand.class);
        verify(assignmentService, times(2)).assign(assignmentCaptor.capture());
        List<RentalDeviceAssignmentCommand> assignments = assignmentCaptor.getAllValues();
        assertEquals(789L, assignments.get(0).rentalOrderItemId());
        assertEquals(101L, assignments.get(0).deviceId());
        assertEquals(TODAY.plusDays(1), assignments.get(0).occupyStartDate());
        assertEquals(TODAY.plusDays(5), assignments.get(0).occupyEndDateExclusive());
        assertEquals("offline-create:456:789:101", assignments.get(0).idempotencyKey());
        assertEquals(102L, assignments.get(1).deviceId());

        InOrder createOrder = inOrder(orderDeliveryMapper, assignmentService);
        createOrder.verify(orderDeliveryMapper).insert(any(RentalOrderDeliveryDO.class));
        createOrder.verify(assignmentService, times(2)).assign(any(RentalDeviceAssignmentCommand.class));
    }

    @Test
    void createManualOrderRejectsDeviceCountMismatchBeforePersistence() {
        stubEnabledModel("P4P");
        RentalManualOrderCreateReqVO reqVO = validReqVO();
        reqVO.getItems().get(0).setDeviceIds(List.of(101L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(reqVO));

        assertEquals(RENTAL_MANUAL_ORDER_INVALID.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(RentalOrderDO.class));
        verify(assignmentService, never()).assign(any());
    }

    @Test
    void createManualOrderRejectsDuplicateDeviceAcrossItems() {
        stubEnabledModel("P4P");
        RentalManualOrderCreateReqVO reqVO = validReqVO();
        RentalManualOrderCreateReqVO.Item duplicate = new RentalManualOrderCreateReqVO.Item();
        duplicate.setModelCode("P4P");
        duplicate.setQuantity(1);
        duplicate.setDeviceIds(List.of(101L));
        duplicate.setRentAmount(10000L);
        reqVO.setItems(List.of(reqVO.getItems().get(0), duplicate));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(reqVO));

        assertEquals(RENTAL_MANUAL_ORDER_INVALID.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(RentalOrderDO.class));
        verify(assignmentService, never()).assign(any());
    }

    @Test
    void createManualOrderMapsAssignmentFailureToManualOrderError() {
        stubEnabledModel("P4P");
        when(assignmentService.assign(any())).thenThrow(new RentalDeviceAssignmentException(
                RentalDeviceAssignmentException.Code.SCHEDULE_CONFLICT, "conflict"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(validReqVO()));

        assertEquals(RENTAL_MANUAL_ORDER_DEVICE_ASSIGN_FAILED.getCode(), ex.getCode());
    }

    @Test
    void createManualOrderReusesExistingCustomerAndRefreshesChangedFields() {
        stubEnabledModel("P4P");
        RentalCustomerDO existing = RentalCustomerDO.builder()
                .id(78L).name("旧名字").mobile("13800000001").wechatId(null).build();
        when(customerMapper.selectByMobile("13800000001")).thenReturn(existing);

        RentalManualOrderCreateRespVO respVO = service.createManualOrder(validReqVO());

        assertEquals(456L, respVO.getId());
        verify(customerMapper, never()).insert(any(RentalCustomerDO.class));
        verify(customerMapper).updateById(existing);
        assertEquals("张三", existing.getName());
        assertEquals("wx-zhangsan", existing.getWechatId());
    }

    @Test
    void createManualOrderLeavesUnchangedCustomerUntouched() {
        stubEnabledModel("P4P");
        RentalCustomerDO existing = RentalCustomerDO.builder()
                .id(78L).name("张三").mobile("13800000001").wechatId("wx-zhangsan").build();
        when(customerMapper.selectByMobile("13800000001")).thenReturn(existing);

        service.createManualOrder(validReqVO());

        verify(customerMapper, never()).insert(any(RentalCustomerDO.class));
        verify(customerMapper, never()).updateById(any(RentalCustomerDO.class));
    }

    @Test
    void createManualOrderRejectsReversedRentalPeriod() {
        RentalManualOrderCreateReqVO reqVO = validReqVO();
        reqVO.setBillableStartDate(TODAY.plusDays(4));
        reqVO.setBillableEndDate(TODAY.plusDays(1));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(reqVO));
        assertEquals(RENTAL_MANUAL_ORDER_INVALID.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(RentalOrderDO.class));
    }

    @Test
    void createManualOrderRejectsStartBeforeToday() {
        RentalManualOrderCreateReqVO reqVO = validReqVO();
        reqVO.setBillableStartDate(TODAY.minusDays(1));
        reqVO.setBillableEndDate(TODAY.plusDays(1));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(reqVO));
        assertEquals(RENTAL_MANUAL_ORDER_INVALID.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(RentalOrderDO.class));
    }

    @Test
    void createManualOrderRejectsUnknownOrDisabledModel() {
        RentalManualOrderCreateReqVO reqVO = validReqVO();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(reqVO));
        assertEquals(RENTAL_MANUAL_ORDER_MODEL_INVALID.getCode(), ex.getCode());

        when(deviceModelMapper.selectByCode("P4P")).thenReturn(RentalDeviceModelDO.builder()
                .id(11L).modelCode("P4P").enabled(false).build());
        ex = assertThrows(ServiceException.class, () -> service.createManualOrder(reqVO));
        assertEquals(RENTAL_MANUAL_ORDER_MODEL_INVALID.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(RentalOrderDO.class));
    }

    @Test
    void createManualOrderRejectsInvalidDeliveryMethod() {
        stubEnabledModel("P4P");
        RentalManualOrderCreateReqVO reqVO = validReqVO();
        reqVO.getDelivery().setMethod("DRONE");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(reqVO));
        assertEquals(RENTAL_MANUAL_ORDER_DELIVERY_METHOD_INVALID.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(RentalOrderDO.class));
    }

    @Test
    void createManualOrderRequiresReceiverInfoForErrand() {
        stubEnabledModel("P4P");
        RentalManualOrderCreateReqVO reqVO = validReqVO();
        reqVO.getDelivery().setReceiverMobile("  ");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createManualOrder(reqVO));
        assertEquals(RENTAL_MANUAL_ORDER_INVALID.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(RentalOrderDO.class));
    }

    @Test
    void confirmOutboundDispatchesEveryAssignedDevice() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(30L).sourceType("OFFLINE").status("PENDING_ALLOCATION").build();
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(order);
        when(orderDeliveryMapper.selectByRentalOrderId(30L)).thenReturn(RentalOrderDeliveryDO.builder()
                .rentalOrderId(30L).deliveryMethod("SELF_DELIVERY").build());
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .id(5L).rentalOrderId(30L).quantity(2).build();
        when(orderItemMapper.selectListByRentalOrderIds(List.of(30L))).thenReturn(List.of(item));
        when(assignmentMapper.countAssignedByOrderItem(5L)).thenReturn(2L);
        RentalDeviceAssignmentDO first = RentalDeviceAssignmentDO.builder()
                .id(91L).rentalOrderId(30L).deviceId(1L).status("ASSIGNED").build();
        RentalDeviceAssignmentDO second = RentalDeviceAssignmentDO.builder()
                .id(92L).rentalOrderId(30L).deviceId(2L).status("ASSIGNED").build();
        when(assignmentMapper.selectActiveListByRentalOrderId(30L))
                .thenReturn(List.of(first, second));

        service.confirmOutbound(30L);

        ArgumentCaptor<RentalDeviceDispatchReqVO> captor =
                ArgumentCaptor.forClass(RentalDeviceDispatchReqVO.class);
        verify(deviceOpsService, times(2)).dispatch(captor.capture());
        List<RentalDeviceDispatchReqVO> dispatched = captor.getAllValues();
        assertEquals(1L, dispatched.get(0).getDeviceId());
        assertEquals(91L, dispatched.get(0).getAssignmentId());
        assertEquals(2L, dispatched.get(1).getDeviceId());
        assertEquals(92L, dispatched.get(1).getAssignmentId());
    }

    @Test
    void confirmOutboundRejectsExpressDelivery() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(30L).sourceType("OFFLINE").status("PENDING_ALLOCATION").build();
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(order);
        when(orderDeliveryMapper.selectByRentalOrderId(30L)).thenReturn(RentalOrderDeliveryDO.builder()
                .rentalOrderId(30L).deliveryMethod("EXPRESS").build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmOutbound(30L));
        assertEquals(RENTAL_MANUAL_ORDER_CONFIRM_EXPRESS.getCode(), ex.getCode());
        verify(deviceOpsService, never()).dispatch(any());
    }

    @Test
    void confirmOutboundRejectsIncompleteAssignment() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(30L).sourceType("OFFLINE").status("PENDING_ALLOCATION").build();
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(order);
        when(orderDeliveryMapper.selectByRentalOrderId(30L)).thenReturn(RentalOrderDeliveryDO.builder()
                .rentalOrderId(30L).deliveryMethod("ERRAND").build());
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .id(5L).rentalOrderId(30L).quantity(2).build();
        when(orderItemMapper.selectListByRentalOrderIds(List.of(30L))).thenReturn(List.of(item));
        when(assignmentMapper.countAssignedByOrderItem(5L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmOutbound(30L));
        assertEquals(RENTAL_MANUAL_ORDER_ASSIGNMENT_INCOMPLETE.getCode(), ex.getCode());
        verify(deviceOpsService, never()).dispatch(any());
    }

    @Test
    void confirmOutboundFailsWhenOrderMissing() {
        when(orderMapper.selectByIdForUpdate(99L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.confirmOutbound(99L));
        assertEquals(RENTAL_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void suggestCustomerDelegatesExactMobileLookup() {
        assertNull(service.suggestCustomer(" "));
        verify(customerMapper, never()).selectByMobile(anyString());

        RentalCustomerDO customer = RentalCustomerDO.builder()
                .id(78L).name("张三").mobile("13800000001").build();
        when(customerMapper.selectByMobile("13800000001")).thenReturn(customer);

        assertEquals(customer, service.suggestCustomer("13800000001"));
    }

}
