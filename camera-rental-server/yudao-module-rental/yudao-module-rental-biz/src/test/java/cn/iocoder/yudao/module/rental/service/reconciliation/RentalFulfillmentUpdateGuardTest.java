package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalFulfillmentUpdateGuardTest {

    @Mock
    private RentalDeviceAssignmentMapper assignmentMapper;
    @Mock
    private RentalDeviceMapper deviceMapper;
    @Mock
    private RentalScheduleMapper scheduleMapper;
    @Mock
    private RentalDeviceLockService deviceLockService;

    private RentalFulfillmentUpdateGuard guard;

    @BeforeEach
    void setUp() {
        guard = new RentalFulfillmentUpdateGuard(
                assignmentMapper, deviceMapper, scheduleMapper, deviceLockService);
    }

    @Test
    void appliesInitialPlanAndModelWhenUnassigned() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = item();
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of());

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(null, candidate(5), RentalRemarkPlanChangeType.INITIAL), "A7M4");

        assertTrue(result.planApplied());
        assertTrue(result.modelApplied());
        assertFalse(result.reviewRequired());
        assertEquals("A7M4", item.getEquipmentModelCode());
        assertEquals(LocalDate.of(2026, 8, 6), item.getOccupyEndDateExclusive());
        assertEquals(LocalDate.of(2026, 8, 5), order.getExpectedSendBackDate());
    }

    @Test
    void extendsAssignedScheduleWithoutConflict() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("ASSIGNED", 30L, 40L);
        RentalScheduleDO schedule = schedule(50L, 30L);
        stubLockedAssignment(assignment, device(30L, "AVAILABLE", "A7M4"));
        stubSchedule(assignment, schedule);

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.planApplied());
        assertFalse(result.reviewRequired());
        assertEquals(LocalDate.of(2026, 8, 6), schedule.getOccupyEndDateExclusive());
        verify(scheduleMapper).updateById(schedule);
    }

    @Test
    void completesScheduleForDispatchedPendingPlanAssignment() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = item();
        item.setEquipmentModelCode("A7M4");
        RentalDeviceAssignmentDO assignment =
                activeAssignment("DISPATCHED_PENDING_PLAN", 30L, 40L);
        assignment.setScheduleId(null);
        stubLockedAssignment(assignment, device(30L, "RENTED", "A7M4"));
        when(scheduleMapper.insert(any(RentalScheduleDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalScheduleDO.class).setId(70L);
            return 1;
        });

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(null, candidate(5), RentalRemarkPlanChangeType.INITIAL), "A7M4");

        assertTrue(result.planApplied());
        assertFalse(result.reviewRequired());
        assertEquals(70L, assignment.getScheduleId());
        assertEquals("DISPATCHED", assignment.getStatus());
        verify(scheduleMapper).insert(any(RentalScheduleDO.class));
        verify(assignmentMapper).updateById(assignment);
    }

    @Test
    void preservesAssignedPlanWhenExtensionConflicts() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("ASSIGNED", 30L, 40L);
        RentalScheduleDO schedule = schedule(50L, 30L);
        stubLockedAssignment(assignment, device(30L, "AVAILABLE", "A7M4"));
        stubSchedule(assignment, schedule);
        when(scheduleMapper.selectEffectiveOverlapsForUpdate(
                30L, LocalDate.of(2026, 7, 22), LocalDate.of(2026, 8, 6)))
                .thenReturn(List.of(RentalScheduleDO.builder().id(999L).build()));

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("ASSIGNED_SCHEDULE_CONFLICT", result.reasonCode());
        assertEquals(LocalDate.of(2026, 8, 3), item.getOccupyEndDateExclusive());
        verify(scheduleMapper, never()).updateById(any(RentalScheduleDO.class));
    }

    @Test
    void preservesDispatchedPlanWhenExtensionConflicts() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("DISPATCHED", 30L, 40L);
        RentalScheduleDO schedule = schedule(50L, 30L);
        stubLockedAssignment(assignment, device(30L, "RENTED", "A7M4"));
        stubSchedule(assignment, schedule);
        when(scheduleMapper.selectEffectiveOverlapsForUpdate(
                30L, LocalDate.of(2026, 7, 22), LocalDate.of(2026, 8, 6)))
                .thenReturn(List.of(RentalScheduleDO.builder().id(999L).build()));

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("DISPATCHED_EXTENSION_CONFLICT", result.reasonCode());
        verify(scheduleMapper, never()).updateById(any(RentalScheduleDO.class));
    }

    @Test
    void earlyReturnOnlyUpdatesExpectedSendBackDate() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("DISPATCHED", 30L, 40L);
        stubLockedAssignment(assignment, device(30L, "RENTED", "A7M4"));

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(1), RentalRemarkPlanChangeType.EARLY_RETURN), "A7M4");

        assertFalse(result.planApplied());
        assertFalse(result.reviewRequired());
        assertEquals(LocalDate.of(2026, 8, 1), item.getExpectedSendBackDate());
        assertEquals(LocalDate.of(2026, 8, 3), item.getOccupyEndDateExclusive());
        verify(scheduleMapper, never()).selectByIdForUpdate(any());
        verify(scheduleMapper, never()).updateById(any(RentalScheduleDO.class));
    }

    @ParameterizedTest
    @EnumSource(value = RentalRemarkPlanChangeType.class,
            names = {"REPLACEMENT", "DAMAGE", "LOSS", "OVERDUE", "LOGISTICS_DELAY"})
    void operationalRemarksAlwaysRequireReview(RentalRemarkPlanChangeType changeType) {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of());

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(2), changeType), "A7M4");

        assertTrue(result.reviewRequired());
        assertTrue(result.reasonCode().startsWith("REMARK_"));
    }

    @Test
    void returnedAndInspectedAssignmentIsImmutable() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO returned = activeAssignment("RETURNED", 30L, 40L);
        returned.setReturnedAt(LocalDateTime.of(2026, 8, 3, 10, 0));
        returned.setInspectionCompletedAt(LocalDateTime.of(2026, 8, 3, 11, 0));
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of(returned));
        when(deviceMapper.selectByIdForUpdate(30L)).thenReturn(device(30L, "AVAILABLE", "A7M4"));
        when(deviceLockService.getActiveLocksForUpdate(30L)).thenReturn(List.of());
        when(assignmentMapper.selectByIdForUpdate(40L)).thenReturn(returned);

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("FULFILLMENT_ALREADY_RETURNED", result.reasonCode());
    }

    @Test
    void settledOrderIsImmutableWithoutLockingAssignments() {
        RentalOrderDO order = order();
        order.setSettledAt(LocalDateTime.of(2026, 8, 4, 12, 0));

        RentalFulfillmentUpdateResult result = guard.apply(
                order, plannedItem(), update(candidate(2), candidate(5),
                        RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("ORDER_FULFILLMENT_IMMUTABLE", result.reasonCode());
        verify(assignmentMapper, never()).selectListByOrderItem(any());
    }

    @Test
    void assignedModelMustMatchConfigurationItemAndPhysicalDevice() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("ASSIGNED", 30L, 40L);
        stubLockedAssignment(assignment, device(30L, "AVAILABLE", "ZV-E10"));

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("ASSIGNED_MODEL_MISMATCH", result.reasonCode());
        verify(scheduleMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void mixedActiveAndReturnedAssignmentsRequireReview() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO active = activeAssignment("ASSIGNED", 30L, 40L);
        RentalDeviceAssignmentDO returned = activeAssignment("RETURNED", 31L, 41L);
        returned.setReturnedAt(LocalDateTime.of(2026, 8, 3, 10, 0));
        returned.setInspectionCompletedAt(LocalDateTime.of(2026, 8, 3, 11, 0));
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of(returned, active));
        when(deviceMapper.selectByIdForUpdate(30L)).thenReturn(device(30L, "AVAILABLE", "A7M4"));
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(device(31L, "AVAILABLE", "A7M4"));
        when(deviceLockService.getActiveLocksForUpdate(30L)).thenReturn(List.of());
        when(deviceLockService.getActiveLocksForUpdate(31L)).thenReturn(List.of());
        when(assignmentMapper.selectByIdForUpdate(40L)).thenReturn(active);
        when(assignmentMapper.selectByIdForUpdate(41L)).thenReturn(returned);

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("MIXED_FULFILLMENT_REVIEW", result.reasonCode());
        verify(scheduleMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void assignedOrderRequiresResolvedModel() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("ASSIGNED", 30L, 40L);
        stubLockedAssignment(assignment, device(30L, "AVAILABLE", "A7M4"));

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), " ");

        assertTrue(result.reviewRequired());
        assertEquals("ASSIGNED_MODEL_MISMATCH", result.reasonCode());
        verify(scheduleMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void assignedOrderRequiresItemModel() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        item.setEquipmentModelCode(null);
        RentalDeviceAssignmentDO assignment = activeAssignment("ASSIGNED", 30L, 40L);
        stubLockedAssignment(assignment, device(30L, "AVAILABLE", "A7M4"));

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("ASSIGNED_MODEL_MISMATCH", result.reasonCode());
        verify(scheduleMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void missingAssignedDeviceRequiresReview() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("ASSIGNED", 30L, 40L);
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of(assignment));
        when(deviceMapper.selectByIdForUpdate(30L)).thenReturn(null);

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("FULFILLMENT_DEVICE_MISSING", result.reasonCode());
        verify(assignmentMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void activeDeviceLockRequiresReview() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO assignment = activeAssignment("ASSIGNED", 30L, 40L);
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of(assignment));
        when(deviceMapper.selectByIdForUpdate(30L)).thenReturn(device(30L, "AVAILABLE", "A7M4"));
        when(deviceLockService.getActiveLocksForUpdate(30L)).thenReturn(List.of(
                RentalDeviceLockDO.builder().deviceId(30L).status("ACTIVE").build()));
        when(assignmentMapper.selectByIdForUpdate(40L)).thenReturn(assignment);

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.reviewRequired());
        assertEquals("FULFILLMENT_DEVICE_LOCKED", result.reasonCode());
        verify(scheduleMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void locksDevicesAssignmentsAndSchedulesInStableOrder() {
        RentalOrderDO order = order();
        RentalOrderItemDO item = plannedItem();
        RentalDeviceAssignmentDO second = activeAssignment("ASSIGNED", 32L, 42L);
        second.setScheduleId(52L);
        RentalDeviceAssignmentDO first = activeAssignment("ASSIGNED", 31L, 41L);
        first.setScheduleId(51L);
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of(second, first));
        when(deviceMapper.selectByIdForUpdate(31L)).thenReturn(device(31L, "AVAILABLE", "A7M4"));
        when(deviceMapper.selectByIdForUpdate(32L)).thenReturn(device(32L, "AVAILABLE", "A7M4"));
        when(deviceLockService.getActiveLocksForUpdate(31L)).thenReturn(List.of());
        when(deviceLockService.getActiveLocksForUpdate(32L)).thenReturn(List.of());
        when(assignmentMapper.selectByIdForUpdate(41L)).thenReturn(first);
        when(assignmentMapper.selectByIdForUpdate(42L)).thenReturn(second);
        when(scheduleMapper.selectByIdForUpdate(51L)).thenReturn(schedule(51L, 31L));
        when(scheduleMapper.selectByIdForUpdate(52L)).thenReturn(schedule(52L, 32L));
        when(scheduleMapper.selectEffectiveOverlapsForUpdate(any(), any(), any())).thenReturn(List.of());

        RentalFulfillmentUpdateResult result = guard.apply(
                order, item, update(candidate(2), candidate(5), RentalRemarkPlanChangeType.EXTENSION), "A7M4");

        assertTrue(result.planApplied());
        InOrder locks = inOrder(deviceMapper, assignmentMapper, scheduleMapper);
        locks.verify(deviceMapper).selectByIdForUpdate(31L);
        locks.verify(deviceMapper).selectByIdForUpdate(32L);
        locks.verify(assignmentMapper).selectByIdForUpdate(41L);
        locks.verify(assignmentMapper).selectByIdForUpdate(42L);
        locks.verify(scheduleMapper).selectByIdForUpdate(51L);
        locks.verify(scheduleMapper).selectByIdForUpdate(52L);
    }

    private void stubLockedAssignment(RentalDeviceAssignmentDO assignment,
                                      RentalDeviceDO device) {
        when(assignmentMapper.selectListByOrderItem(20L)).thenReturn(List.of(assignment));
        when(deviceMapper.selectByIdForUpdate(assignment.getDeviceId())).thenReturn(device);
        when(deviceLockService.getActiveLocksForUpdate(assignment.getDeviceId())).thenReturn(List.of());
        when(assignmentMapper.selectByIdForUpdate(assignment.getId())).thenReturn(assignment);
    }

    private void stubSchedule(RentalDeviceAssignmentDO assignment,
                              RentalScheduleDO schedule) {
        when(scheduleMapper.selectByIdForUpdate(assignment.getScheduleId())).thenReturn(schedule);
        when(scheduleMapper.selectEffectiveOverlapsForUpdate(any(), any(), any())).thenReturn(List.of());
    }

    private static RentalOrderDO order() {
        return RentalOrderDO.builder().id(10L).status("PENDING_ALLOCATION").build();
    }

    private static RentalOrderItemDO item() {
        return RentalOrderItemDO.builder().id(20L).rentalOrderId(10L).quantity(1).build();
    }

    private static RentalOrderItemDO plannedItem() {
        RentalOrderItemDO item = item();
        item.setEquipmentModelCode("A7M4");
        item.setBillableStartDate(LocalDate.of(2026, 7, 25));
        item.setBillableEndDate(LocalDate.of(2026, 8, 2));
        item.setOccupyStartDate(LocalDate.of(2026, 7, 22));
        item.setOccupyEndDateExclusive(LocalDate.of(2026, 8, 3));
        item.setExpectedSendBackDate(LocalDate.of(2026, 8, 2));
        return item;
    }

    private static RentalDeviceAssignmentDO activeAssignment(String status, Long deviceId, Long assignmentId) {
        return RentalDeviceAssignmentDO.builder()
                .id(assignmentId)
                .rentalOrderId(10L)
                .rentalOrderItemId(20L)
                .deviceId(deviceId)
                .scheduleId(50L)
                .status(status)
                .build();
    }

    private static RentalDeviceDO device(Long id, String status, String modelCode) {
        return RentalDeviceDO.builder()
                .id(id)
                .enabled(true)
                .status(status)
                .equipmentModelCode(modelCode)
                .build();
    }

    private static RentalScheduleDO schedule(Long id, Long deviceId) {
        return RentalScheduleDO.builder()
                .id(id)
                .deviceId(deviceId)
                .rentalOrderId(10L)
                .rentalOrderItemId(20L)
                .status("EFFECTIVE")
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 3))
                .build();
    }

    private static SellerRemarkRentalPeriod candidate(int returnDay) {
        return SellerRemarkRentalPeriod.success(
                "SELLER_REMARK_V7_AI",
                LocalDate.of(2026, 7, 25),
                LocalDate.of(2026, 8, returnDay),
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 24),
                LocalDate.of(2026, 8, returnDay));
    }

    private static RentalRemarkPlanUpdate update(SellerRemarkRentalPeriod previous,
                                                 SellerRemarkRentalPeriod candidate,
                                                 RentalRemarkPlanChangeType type) {
        return new RentalRemarkPlanUpdate(previous, candidate, type);
    }

}
