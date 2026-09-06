package cn.iocoder.yudao.module.rental.service;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceOpsService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalOrderPreparationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalDeviceAssignmentServiceImplTest {

    @Mock
    private RentalDeviceAssignmentMapper assignmentMapper;
    @Mock
    private RentalDeviceMapper deviceMapper;
    @Mock
    private RentalOrderItemMapper orderItemMapper;
    @Mock
    private RentalOrderMapper orderMapper;
    @Mock
    private RentalScheduleMapper scheduleMapper;
    @Mock
    private RentalDeviceLockService deviceLockService;
    @Mock
    private RentalOrderDeliveryMapper orderDeliveryMapper;
    @Mock
    private RentalDeviceOpsService deviceOpsService;

    private RentalDeviceAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new RentalDeviceAssignmentServiceImpl(assignmentMapper, deviceMapper, orderItemMapper, orderMapper,
                scheduleMapper, deviceLockService, new RentalOrderPreparationPolicy(),
                orderDeliveryMapper, deviceOpsService);
    }

    @Test
    void shouldCreateOneAssignmentAndHalfOpenScheduleAfterLocks() {
        stubAssignableCommand();
        doAnswer(invocation -> {
            invocation.getArgument(0, RentalScheduleDO.class).setId(71L);
            return 1;
        }).when(scheduleMapper).insert(any(RentalScheduleDO.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, RentalDeviceAssignmentDO.class).setId(81L);
            return 1;
        }).when(assignmentMapper).insert(any(RentalDeviceAssignmentDO.class));

        RentalDeviceAssignmentResult result = service.assign(command());

        assertEquals(81L, result.assignmentId());
        assertEquals(71L, result.scheduleId());
        assertEquals(31L, result.deviceId());
        InOrder locks = inOrder(deviceMapper, orderItemMapper, orderMapper, scheduleMapper);
        locks.verify(orderItemMapper).selectById(21L);
        locks.verify(orderMapper).selectByIdForUpdate(11L);
        locks.verify(orderItemMapper).selectByIdForUpdate(21L);
        locks.verify(deviceMapper).selectByIdForUpdate(31L);
        locks.verify(scheduleMapper).selectEffectiveOverlapsForUpdate(31L, LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 31));
        verify(scheduleMapper).insert(any(RentalScheduleDO.class));
        verify(assignmentMapper).insert(any(RentalDeviceAssignmentDO.class));
    }

    @Test
    void shouldReturnExistingAssignmentForIdempotentReplay() {
        when(assignmentMapper.selectByIdempotencyKeyForUpdate("assign-1"))
                .thenReturn(RentalDeviceAssignmentDO.builder().id(81L).scheduleId(71L).deviceId(31L).build());
        when(scheduleMapper.selectById(71L)).thenReturn(RentalScheduleDO.builder().rentalOrderItemId(21L)
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 31)).build());

        RentalDeviceAssignmentResult result = service.assign(command());

        assertEquals(81L, result.assignmentId());
        assertEquals(LocalDate.of(2026, 7, 22), result.occupyStartDate());
        verify(deviceMapper, never()).selectByIdForUpdate(anyLong());
        verify(scheduleMapper, never()).insert(any(RentalScheduleDO.class));
        verify(assignmentMapper, never()).insert(any(RentalDeviceAssignmentDO.class));
    }

    @Test
    void shouldCreatePendingPlanAssignmentWithoutSchedule() {
        RentalOrderItemDO item = RentalOrderItemDO.builder().id(21L)
                .rentalOrderId(11L).quantity(1).equipmentModelCode("A7M4").build();
        RentalOrderDO order = RentalOrderDO.builder().id(11L)
                .status("PENDING_ALLOCATION").preparationStatus("WAITING_REMARK").build();
        when(orderItemMapper.selectById(21L)).thenReturn(item);
        when(orderMapper.selectByIdForUpdate(11L)).thenReturn(order);
        when(orderItemMapper.selectByIdForUpdate(21L)).thenReturn(item);
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(RentalDeviceDO.builder().id(31L)
                .enabled(true).status("AVAILABLE").equipmentModelCode("A7M4").build());
        when(deviceLockService.getActiveLocksForUpdate(31L)).thenReturn(List.of());
        when(assignmentMapper.countAssignedByOrderItem(21L)).thenReturn(0L);
        when(assignmentMapper.insert(any(RentalDeviceAssignmentDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalDeviceAssignmentDO.class).setId(81L);
            return 1;
        });

        RentalDeviceAssignmentResult result = service.assignPendingPlan(21L, 31L, "pending-1");

        assertEquals(81L, result.assignmentId());
        assertEquals(31L, result.deviceId());
        assertEquals(null, result.scheduleId());
        verify(scheduleMapper, never()).insert(any(RentalScheduleDO.class));
        verify(assignmentMapper).insert(any(RentalDeviceAssignmentDO.class));
    }

    @Test
    void shouldRejectAnIdempotencyKeyReusedForAnotherPeriod() {
        when(assignmentMapper.selectByIdempotencyKeyForUpdate("assign-1"))
                .thenReturn(RentalDeviceAssignmentDO.builder().id(81L).scheduleId(71L).deviceId(31L).build());
        when(scheduleMapper.selectById(71L)).thenReturn(RentalScheduleDO.builder().rentalOrderItemId(21L)
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 31)).build());
        RentalDeviceAssignmentCommand changedPeriod = new RentalDeviceAssignmentCommand(21L, 31L,
                LocalDate.of(2026, 7, 23), LocalDate.of(2026, 7, 31), "assign-1");

        RentalDeviceAssignmentException exception = assertThrows(RentalDeviceAssignmentException.class,
                () -> service.assign(changedPeriod));

        assertEquals(RentalDeviceAssignmentException.Code.IDEMPOTENCY_KEY_REUSED, exception.getCode());
        verify(deviceMapper, never()).selectByIdForUpdate(anyLong());
    }

    @Test
    void shouldRejectAnOverlappingEffectiveScheduleWithoutPartialWrites() {
        stubAssignableCommand();
        when(scheduleMapper.selectEffectiveOverlapsForUpdate(31L, LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 31))).thenReturn(List.of(RentalScheduleDO.builder()
                .occupyStartDate(LocalDate.of(2026, 7, 25))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 1))
                .build()));

        RentalDeviceAssignmentException exception = assertThrows(RentalDeviceAssignmentException.class,
                () -> service.assign(command()));

        assertEquals(RentalDeviceAssignmentException.Code.SCHEDULE_CONFLICT, exception.getCode());
        verify(scheduleMapper, never()).insert(any(RentalScheduleDO.class));
        verify(assignmentMapper, never()).insert(any(RentalDeviceAssignmentDO.class));
    }

    @Test
    void shouldAllowAdjacentHalfOpenSchedules() {
        assertFalse(RentalDeviceAssignmentServiceImpl.overlaps(LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 4)));
        assertFalse(RentalDeviceAssignmentServiceImpl.overlaps(LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 8), LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 4)));
        assertTrue(RentalDeviceAssignmentServiceImpl.overlaps(LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 4)));
    }

    @Test
    void shouldRejectMismatchedDeviceModelBeforeScheduling() {
        RentalOrderItemDO item = readyItem();
        when(orderItemMapper.selectById(21L)).thenReturn(item);
        when(orderMapper.selectByIdForUpdate(11L)).thenReturn(RentalOrderDO.builder().id(11L)
                .status("PENDING_ALLOCATION").preparationStatus("READY").build());
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(RentalDeviceDO.builder().id(31L).enabled(true)
                .status("AVAILABLE").equipmentModelCode("ZV-E10").build());
        when(orderItemMapper.selectByIdForUpdate(21L)).thenReturn(item);

        RentalDeviceAssignmentException exception = assertThrows(RentalDeviceAssignmentException.class,
                () -> service.assign(command()));

        assertEquals(RentalDeviceAssignmentException.Code.DEVICE_MODEL_MISMATCH, exception.getCode());
        verify(orderMapper).selectByIdForUpdate(11L);
        verify(scheduleMapper, never()).insert(any(RentalScheduleDO.class));
    }

    @Test
    void shouldRejectFullyAssignedOrderItem() {
        stubAssignableCommand();
        when(assignmentMapper.countAssignedByOrderItem(21L)).thenReturn(1L);

        RentalDeviceAssignmentException exception = assertThrows(RentalDeviceAssignmentException.class,
                () -> service.assign(command()));

        assertEquals(RentalDeviceAssignmentException.Code.ITEM_ALREADY_FULLY_ASSIGNED, exception.getCode());
        verify(scheduleMapper, never()).selectEffectiveOverlapsForUpdate(anyLong(), any(), any());
        verify(assignmentMapper, never()).insert(any(RentalDeviceAssignmentDO.class));
    }

    @Test
    void shouldNotCreateAssignmentWhenSchedulePersistenceFails() throws NoSuchMethodException {
        stubAssignableCommand();
        when(scheduleMapper.insert(any(RentalScheduleDO.class))).thenThrow(new IllegalStateException("db failure"));

        assertThrows(IllegalStateException.class, () -> service.assign(command()));

        verify(assignmentMapper, never()).insert(any(RentalDeviceAssignmentDO.class));
        Transactional transactional = RentalDeviceAssignmentServiceImpl.class.getMethod("assign",
                RentalDeviceAssignmentCommand.class).getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

    @Test
    void shouldRejectOrderThatIsNotPreparationReady() {
        RentalOrderItemDO item = readyItem();
        when(orderItemMapper.selectById(21L)).thenReturn(item);
        when(orderItemMapper.selectByIdForUpdate(21L)).thenReturn(item);
        when(orderMapper.selectByIdForUpdate(11L)).thenReturn(RentalOrderDO.builder().id(11L)
                .status("PENDING_ALLOCATION").preparationStatus("WAITING_REMARK")
                .preparationReasonCode("MISSING_REMARK").build());

        RentalDeviceAssignmentException exception = assertThrows(
                RentalDeviceAssignmentException.class, () -> service.assign(command()));

        assertEquals(RentalDeviceAssignmentException.Code.ORDER_NOT_READY, exception.getCode());
        verify(scheduleMapper, never()).selectEffectiveOverlapsForUpdate(anyLong(), any(), any());
        verify(scheduleMapper, never()).insert(any(RentalScheduleDO.class));
    }

    @Test
    void shouldDispatchImmediatelyForOfflineErrandOrder() {
        RentalDeviceAssignmentService realService = serviceWithRealOps();
        RentalDeviceDO device = RentalDeviceDO.builder().id(31L).enabled(true)
                .status("AVAILABLE").equipmentModelCode("A7M4").build();
        RentalDeviceAssignmentDO[] inserted = stubAssignableOfflineOrder(device, "ERRAND");
        when(assignmentMapper.selectByIdForUpdate(81L)).thenAnswer(invocation -> inserted[0]);

        RentalDeviceAssignmentResult result = realService.assign(command());

        assertEquals(81L, result.assignmentId());
        assertEquals("DISPATCHED", inserted[0].getStatus());
        assertEquals("RENTED", device.getStatus());
        verify(deviceMapper).updateById(device);
        verify(assignmentMapper).updateById(inserted[0]);
    }

    @Test
    void shouldDispatchImmediatelyForOfflineSelfDeliveryOrder() {
        RentalDeviceAssignmentService realService = serviceWithRealOps();
        RentalDeviceDO device = RentalDeviceDO.builder().id(31L).enabled(true)
                .status("AVAILABLE").equipmentModelCode("A7M4").build();
        RentalDeviceAssignmentDO[] inserted = stubAssignableOfflineOrder(device, "SELF_DELIVERY");
        when(assignmentMapper.selectByIdForUpdate(81L)).thenAnswer(invocation -> inserted[0]);

        RentalDeviceAssignmentResult result = realService.assign(command());

        assertEquals(81L, result.assignmentId());
        assertEquals("DISPATCHED", inserted[0].getStatus());
        assertEquals("RENTED", device.getStatus());
    }

    @Test
    void shouldKeepAssignOnlyForOfflineExpressOrder() {
        RentalDeviceAssignmentDO[] inserted = stubAssignableOfflineOrder("EXPRESS");

        RentalDeviceAssignmentResult result = service.assign(command());

        assertEquals(81L, result.assignmentId());
        assertEquals("ASSIGNED", inserted[0].getStatus());
        verify(deviceOpsService, never()).dispatch(any());
        verify(deviceMapper, never()).updateById(any(RentalDeviceDO.class));
    }

    @Test
    void shouldKeepAssignOnlyWhenOrderHasNoDeliveryRecord() {
        RentalDeviceAssignmentDO[] inserted = stubAssignableOfflineOrder(null);

        RentalDeviceAssignmentResult result = service.assign(command());

        assertEquals(81L, result.assignmentId());
        assertEquals("ASSIGNED", inserted[0].getStatus());
        verify(deviceOpsService, never()).dispatch(any());
    }

    @Test
    void shouldPropagateDispatchFailureSoAssignmentRollsBack() {
        stubAssignableOfflineOrder("ERRAND");
        when(deviceOpsService.dispatch(any())).thenThrow(new IllegalStateException("dispatch failed"));

        assertThrows(IllegalStateException.class, () -> service.assign(command()));

        verify(assignmentMapper).insert(any(RentalDeviceAssignmentDO.class));
        verify(deviceMapper, never()).updateById(any(RentalDeviceDO.class));
    }

    @Test
    void shouldDispatchPendingPlanAssignmentForOfflineErrandOrder() {
        RentalOrderItemDO item = RentalOrderItemDO.builder().id(21L)
                .rentalOrderId(11L).quantity(1).equipmentModelCode("A7M4").build();
        when(orderItemMapper.selectById(21L)).thenReturn(item);
        when(orderMapper.selectByIdForUpdate(11L)).thenReturn(RentalOrderDO.builder().id(11L)
                .status("PENDING_ALLOCATION").preparationStatus("WAITING_REMARK").build());
        when(orderItemMapper.selectByIdForUpdate(21L)).thenReturn(item);
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(RentalDeviceDO.builder().id(31L)
                .enabled(true).status("AVAILABLE").equipmentModelCode("A7M4").build());
        when(deviceLockService.getActiveLocksForUpdate(31L)).thenReturn(List.of());
        when(assignmentMapper.countAssignedByOrderItem(21L)).thenReturn(0L);
        when(assignmentMapper.insert(any(RentalDeviceAssignmentDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalDeviceAssignmentDO.class).setId(81L);
            return 1;
        });
        when(orderDeliveryMapper.selectByRentalOrderId(11L)).thenReturn(RentalOrderDeliveryDO.builder()
                .rentalOrderId(11L).deliveryMethod("ERRAND").build());

        RentalDeviceAssignmentResult result = service.assignPendingPlan(21L, 31L, "pending-1");

        assertEquals(81L, result.assignmentId());
        ArgumentCaptor<RentalDeviceDispatchReqVO> captor =
                ArgumentCaptor.forClass(RentalDeviceDispatchReqVO.class);
        verify(deviceOpsService).dispatch(captor.capture());
        assertEquals(31L, captor.getValue().getDeviceId());
        assertEquals(81L, captor.getValue().getAssignmentId());
    }

    private RentalDeviceAssignmentService serviceWithRealOps() {
        RentalDeviceOpsService realOpsService =
                new RentalDeviceOpsService(deviceMapper, assignmentMapper, scheduleMapper, deviceLockService);
        return new RentalDeviceAssignmentServiceImpl(assignmentMapper, deviceMapper, orderItemMapper, orderMapper,
                scheduleMapper, deviceLockService, new RentalOrderPreparationPolicy(),
                orderDeliveryMapper, realOpsService);
    }

    private RentalDeviceAssignmentDO[] stubAssignableOfflineOrder(String deliveryMethod) {
        return stubAssignableOfflineOrder(RentalDeviceDO.builder().id(31L).enabled(true)
                .status("AVAILABLE").equipmentModelCode("A7M4").build(), deliveryMethod);
    }

    private RentalDeviceAssignmentDO[] stubAssignableOfflineOrder(RentalDeviceDO device,
                                                                  String deliveryMethod) {
        RentalOrderItemDO item = readyItem();
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(device);
        when(deviceLockService.getActiveLocksForUpdate(31L)).thenReturn(List.of());
        when(orderItemMapper.selectById(21L)).thenReturn(item);
        when(orderItemMapper.selectByIdForUpdate(21L)).thenReturn(item);
        when(orderMapper.selectByIdForUpdate(11L)).thenReturn(RentalOrderDO.builder().id(11L)
                .status("PENDING_ALLOCATION").preparationStatus("READY").build());
        when(assignmentMapper.countAssignedByOrderItem(21L)).thenReturn(0L);
        RentalDeviceAssignmentDO[] inserted = new RentalDeviceAssignmentDO[1];
        doAnswer(invocation -> {
            RentalDeviceAssignmentDO value = invocation.getArgument(0);
            value.setId(81L);
            inserted[0] = value;
            return 1;
        }).when(assignmentMapper).insert(any(RentalDeviceAssignmentDO.class));
        when(orderDeliveryMapper.selectByRentalOrderId(11L)).thenReturn(deliveryMethod == null ? null
                : RentalOrderDeliveryDO.builder().rentalOrderId(11L).deliveryMethod(deliveryMethod).build());
        return inserted;
    }

    private void stubAssignableCommand() {
        RentalOrderItemDO item = readyItem();
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(RentalDeviceDO.builder().id(31L).enabled(true)
                .status("AVAILABLE").equipmentModelCode("A7M4").build());
        when(deviceLockService.getActiveLocksForUpdate(31L)).thenReturn(List.of());
        when(orderItemMapper.selectById(21L)).thenReturn(item);
        when(orderItemMapper.selectByIdForUpdate(21L)).thenReturn(item);
        when(orderMapper.selectByIdForUpdate(11L)).thenReturn(RentalOrderDO.builder().id(11L)
                .status("PENDING_ALLOCATION").preparationStatus("READY").build());
        when(assignmentMapper.countAssignedByOrderItem(21L)).thenReturn(0L);
    }

    @Test
    void shouldRejectDeviceThatBecomesLockedBeforeFinalAssignment() {
        RentalOrderItemDO item = readyItem();
        when(orderItemMapper.selectById(21L)).thenReturn(item);
        when(orderMapper.selectByIdForUpdate(11L)).thenReturn(RentalOrderDO.builder().id(11L)
                .status("PENDING_ALLOCATION").preparationStatus("READY").build());
        when(orderItemMapper.selectByIdForUpdate(21L)).thenReturn(item);
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(RentalDeviceDO.builder().id(31L).enabled(true)
                .status("AVAILABLE").equipmentModelCode("A7M4").build());
        when(deviceLockService.getActiveLocksForUpdate(31L)).thenReturn(List.of(
                cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO.builder()
                        .lockType("ORDER_HOLD").status("ACTIVE").build()));

        RentalDeviceAssignmentException exception = assertThrows(RentalDeviceAssignmentException.class,
                () -> service.assign(command()));

        assertEquals(RentalDeviceAssignmentException.Code.DEVICE_LOCKED, exception.getCode());
        verify(orderItemMapper).selectByIdForUpdate(21L);
        verify(scheduleMapper, never()).insert(any(RentalScheduleDO.class));
    }

    private static RentalOrderItemDO readyItem() {
        return RentalOrderItemDO.builder().id(21L)
                .rentalOrderId(11L).quantity(1).equipmentModelCode("A7M4")
                .billableStartDate(LocalDate.of(2026, 7, 25))
                .billableEndDate(LocalDate.of(2026, 7, 30))
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 31))
                .build();
    }

    private RentalDeviceAssignmentCommand command() {
        return new RentalDeviceAssignmentCommand(21L, 31L, LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 31), "assign-1");
    }

}
