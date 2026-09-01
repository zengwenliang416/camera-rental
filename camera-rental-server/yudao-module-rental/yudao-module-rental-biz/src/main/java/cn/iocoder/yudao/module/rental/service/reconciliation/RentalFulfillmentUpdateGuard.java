package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class RentalFulfillmentUpdateGuard {

    private static final Set<String> ACTIVE_ASSIGNMENT_STATUSES = Set.of("ASSIGNED", "DISPATCHED");
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final RentalDeviceLockService deviceLockService;

    public RentalFulfillmentUpdateGuard(RentalDeviceAssignmentMapper assignmentMapper,
                                        RentalDeviceMapper deviceMapper,
                                        RentalScheduleMapper scheduleMapper,
                                        RentalDeviceLockService deviceLockService) {
        this.assignmentMapper = assignmentMapper;
        this.deviceMapper = deviceMapper;
        this.scheduleMapper = scheduleMapper;
        this.deviceLockService = deviceLockService;
    }

    public RentalFulfillmentUpdateResult apply(RentalOrderDO order,
                                               RentalOrderItemDO item,
                                               RentalRemarkPlanUpdate planUpdate,
                                               String resolvedModelCode) {
        if (order.getSettledAt() != null || "CANCELED".equals(order.getStatus())) {
            return RentalFulfillmentUpdateResult.review("ORDER_FULFILLMENT_IMMUTABLE");
        }
        AssignmentLockResult assignmentLock = lockAssignments(item);
        if (assignmentLock.reasonCode() != null) {
            return RentalFulfillmentUpdateResult.review(assignmentLock.reasonCode());
        }
        List<RentalDeviceAssignmentDO> assignments = assignmentLock.assignments();
        List<RentalDeviceAssignmentDO> activeAssignments = assignments.stream()
                .filter(value -> ACTIVE_ASSIGNMENT_STATUSES.contains(value.getStatus()))
                .toList();
        boolean hasReturned = assignments.stream().anyMatch(value -> "RETURNED".equals(value.getStatus()));
        if (hasReturned && !activeAssignments.isEmpty()) {
            return RentalFulfillmentUpdateResult.review("MIXED_FULFILLMENT_REVIEW");
        }
        if (activeAssignments.isEmpty()) {
            if (hasReturned) {
                return RentalFulfillmentUpdateResult.review("FULFILLMENT_ALREADY_RETURNED");
            }
            return applyUnassigned(order, item, planUpdate, resolvedModelCode);
        }
        if (!StringUtils.hasText(resolvedModelCode)
                || !StringUtils.hasText(item.getEquipmentModelCode())
                || !item.getEquipmentModelCode().equals(resolvedModelCode)
                || activeAssignments.stream().map(RentalDeviceAssignmentDO::getDeviceId)
                .map(assignmentLock.devices()::get)
                .anyMatch(device -> device == null
                        || !Boolean.TRUE.equals(device.getEnabled())
                        || !StringUtils.hasText(device.getEquipmentModelCode())
                        || !resolvedModelCode.equals(device.getEquipmentModelCode()))) {
            return RentalFulfillmentUpdateResult.review("ASSIGNED_MODEL_MISMATCH");
        }

        SellerRemarkRentalPeriod candidate = effectiveCandidate(planUpdate);
        if (candidate == null) {
            return RentalFulfillmentUpdateResult.applied(false, false);
        }
        RentalRemarkPlanChangeType changeType = planUpdate.changeType();
        String operationalReason = operationalReviewReason(changeType);
        if (operationalReason != null) {
            return RentalFulfillmentUpdateResult.review(operationalReason);
        }
        if (changeType == RentalRemarkPlanChangeType.AMBIGUOUS) {
            return RentalFulfillmentUpdateResult.review("REMARK_CHANGE_AMBIGUOUS");
        }
        boolean dispatched = activeAssignments.stream()
                .anyMatch(value -> "DISPATCHED".equals(value.getStatus()));
        if (changeType == RentalRemarkPlanChangeType.EARLY_RETURN) {
            applyExpectedSendBack(candidate, order, item);
            return RentalFulfillmentUpdateResult.applied(false, false);
        }
        if (dispatched && changeType != RentalRemarkPlanChangeType.EXTENSION
                && changeType != RentalRemarkPlanChangeType.UNCHANGED) {
            return RentalFulfillmentUpdateResult.review("DISPATCHED_PLAN_CHANGE_REVIEW");
        }
        if (changeType == RentalRemarkPlanChangeType.UNCHANGED) {
            return RentalFulfillmentUpdateResult.applied(true, false);
        }

        String conflictReason = updateSchedules(activeAssignments, candidate, dispatched);
        if (conflictReason != null) {
            return RentalFulfillmentUpdateResult.review(conflictReason);
        }
        applyPlan(candidate, order, item);
        return RentalFulfillmentUpdateResult.applied(true, false);
    }

    private RentalFulfillmentUpdateResult applyUnassigned(RentalOrderDO order,
                                                          RentalOrderItemDO item,
                                                          RentalRemarkPlanUpdate planUpdate,
                                                          String resolvedModelCode) {
        boolean modelApplied = !Objects.equals(item.getEquipmentModelCode(), resolvedModelCode);
        SellerRemarkRentalPeriod candidate = effectiveCandidate(planUpdate);
        if (candidate != null) {
            RentalRemarkPlanChangeType changeType = planUpdate.changeType();
            String operationalReason = operationalReviewReason(changeType);
            if (operationalReason != null) {
                return RentalFulfillmentUpdateResult.review(operationalReason);
            }
            if (changeType == RentalRemarkPlanChangeType.AMBIGUOUS) {
                return RentalFulfillmentUpdateResult.review("REMARK_CHANGE_AMBIGUOUS");
            }
            if (changeType == RentalRemarkPlanChangeType.EARLY_RETURN) {
                item.setEquipmentModelCode(resolvedModelCode);
                applyExpectedSendBack(candidate, order, item);
                return RentalFulfillmentUpdateResult.applied(false, modelApplied);
            }
        }
        item.setEquipmentModelCode(resolvedModelCode);
        if (candidate == null) {
            return RentalFulfillmentUpdateResult.applied(false, modelApplied);
        }
        applyPlan(candidate, order, item);
        return RentalFulfillmentUpdateResult.applied(true, modelApplied);
    }

    private String updateSchedules(List<RentalDeviceAssignmentDO> assignments,
                                   SellerRemarkRentalPeriod candidate,
                                   boolean dispatched) {
        Set<Long> ownScheduleIds = new HashSet<>();
        for (RentalDeviceAssignmentDO assignment : assignments) {
            if (assignment.getScheduleId() == null) {
                return "FULFILLMENT_SCHEDULE_MISSING";
            }
            ownScheduleIds.add(assignment.getScheduleId());
        }
        List<Long> orderedScheduleIds = ownScheduleIds.stream().sorted().toList();
        Map<Long, RentalScheduleDO> schedules = new HashMap<>();
        for (Long scheduleId : orderedScheduleIds) {
            RentalScheduleDO schedule = scheduleMapper.selectByIdForUpdate(scheduleId);
            if (schedule == null) {
                return "FULFILLMENT_SCHEDULE_MISSING";
            }
            schedules.put(scheduleId, schedule);
        }
        List<RentalDeviceAssignmentDO> orderedAssignments = assignments.stream()
                .sorted(Comparator.comparing(RentalDeviceAssignmentDO::getDeviceId)
                        .thenComparing(RentalDeviceAssignmentDO::getId))
                .toList();
        for (RentalDeviceAssignmentDO assignment : orderedAssignments) {
            RentalScheduleDO schedule = schedules.get(assignment.getScheduleId());
            if (!matchesAssignment(schedule, assignment)) {
                return "FULFILLMENT_SCHEDULE_MISMATCH";
            }
            List<RentalScheduleDO> conflicts = scheduleMapper.selectEffectiveOverlapsForUpdate(
                    assignment.getDeviceId(), candidate.occupyStartDate(), candidate.occupyEndDateExclusive());
            if (conflicts.stream().anyMatch(value -> !ownScheduleIds.contains(value.getId()))) {
                return dispatched ? "DISPATCHED_EXTENSION_CONFLICT" : "ASSIGNED_SCHEDULE_CONFLICT";
            }
        }
        for (Long scheduleId : orderedScheduleIds) {
            RentalScheduleDO schedule = schedules.get(scheduleId);
            schedule.setOccupyStartDate(candidate.occupyStartDate());
            schedule.setOccupyEndDateExclusive(candidate.occupyEndDateExclusive());
            scheduleMapper.updateById(schedule);
        }
        return null;
    }

    private static boolean matchesAssignment(RentalScheduleDO schedule,
                                             RentalDeviceAssignmentDO assignment) {
        return schedule != null
                && "EFFECTIVE".equals(schedule.getStatus())
                && Objects.equals(schedule.getId(), assignment.getScheduleId())
                && Objects.equals(schedule.getDeviceId(), assignment.getDeviceId())
                && Objects.equals(schedule.getRentalOrderId(), assignment.getRentalOrderId())
                && Objects.equals(schedule.getRentalOrderItemId(), assignment.getRentalOrderItemId());
    }

    private static SellerRemarkRentalPeriod effectiveCandidate(RentalRemarkPlanUpdate planUpdate) {
        SellerRemarkRentalPeriod candidate = planUpdate == null ? null : planUpdate.candidatePlan();
        if (candidate == null || !candidate.isSuccess()
                || candidate.billableStartDate() == null || candidate.billableEndDate() == null
                || candidate.shipDate() == null || candidate.returnDate() == null) {
            return null;
        }
        return candidate;
    }

    private static void applyPlan(SellerRemarkRentalPeriod candidate,
                                  RentalOrderDO order,
                                  RentalOrderItemDO item) {
        LocalDate occupyEndExclusive = candidate.returnDate().plusDays(1);
        order.setBillableStartDate(candidate.billableStartDate());
        order.setBillableEndDate(candidate.billableEndDate());
        order.setOccupyStartDate(candidate.shipDate());
        order.setOccupyEndDateExclusive(occupyEndExclusive);
        order.setConversionVersion(candidate.version());
        item.setBillableStartDate(candidate.billableStartDate());
        item.setBillableEndDate(candidate.billableEndDate());
        item.setOccupyStartDate(candidate.shipDate());
        item.setOccupyEndDateExclusive(occupyEndExclusive);
        order.setExpectedSendBackDate(candidate.returnDate());
        item.setExpectedSendBackDate(candidate.returnDate());
    }

    private static void applyExpectedSendBack(SellerRemarkRentalPeriod candidate,
                                              RentalOrderDO order,
                                              RentalOrderItemDO item) {
        order.setExpectedSendBackDate(candidate.returnDate());
        item.setExpectedSendBackDate(candidate.returnDate());
    }

    private AssignmentLockResult lockAssignments(RentalOrderItemDO item) {
        if (item.getId() == null) {
            return AssignmentLockResult.success(List.of(), Map.of());
        }
        List<RentalDeviceAssignmentDO> snapshots = assignmentMapper.selectListByOrderItem(item.getId()).stream()
                .sorted(Comparator.comparing(RentalDeviceAssignmentDO::getId,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        List<Long> deviceIds = snapshots.stream()
                .map(RentalDeviceAssignmentDO::getDeviceId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        Map<Long, RentalDeviceDO> devices = new HashMap<>();
        for (Long deviceId : deviceIds) {
            RentalDeviceDO device = deviceMapper.selectByIdForUpdate(deviceId);
            if (device == null) {
                return AssignmentLockResult.review("FULFILLMENT_DEVICE_MISSING");
            }
            devices.put(deviceId, device);
        }
        Set<Long> devicesWithActiveLocks = new HashSet<>();
        for (Long deviceId : deviceIds) {
            if (!deviceLockService.getActiveLocksForUpdate(deviceId).isEmpty()) {
                devicesWithActiveLocks.add(deviceId);
            }
        }
        List<RentalDeviceAssignmentDO> locked = new ArrayList<>();
        for (RentalDeviceAssignmentDO snapshot : snapshots) {
            RentalDeviceAssignmentDO current = assignmentMapper.selectByIdForUpdate(snapshot.getId());
            if (current == null || !Objects.equals(current.getRentalOrderItemId(), item.getId())
                    || !Objects.equals(current.getDeviceId(), snapshot.getDeviceId())) {
                return AssignmentLockResult.review("FULFILLMENT_ASSIGNMENT_CHANGED");
            }
            String consistencyError = assignmentConsistencyError(current, devices.get(current.getDeviceId()));
            if (consistencyError != null) {
                return AssignmentLockResult.review(consistencyError);
            }
            if (ACTIVE_ASSIGNMENT_STATUSES.contains(current.getStatus())
                    && devicesWithActiveLocks.contains(current.getDeviceId())) {
                return AssignmentLockResult.review("FULFILLMENT_DEVICE_LOCKED");
            }
            if (!ACTIVE_ASSIGNMENT_STATUSES.contains(current.getStatus())
                    && !"RETURNED".equals(current.getStatus())
                    && !"CANCELED".equals(current.getStatus())) {
                return AssignmentLockResult.review("FULFILLMENT_ASSIGNMENT_STATUS_REVIEW");
            }
            locked.add(current);
        }
        return AssignmentLockResult.success(locked, devices);
    }

    private static String assignmentConsistencyError(RentalDeviceAssignmentDO assignment,
                                                     RentalDeviceDO device) {
        if (device == null) {
            return "FULFILLMENT_DEVICE_MISSING";
        }
        if ("ASSIGNED".equals(assignment.getStatus()) && !"AVAILABLE".equals(device.getStatus())) {
            return "ASSIGNED_DEVICE_STATE_MISMATCH";
        }
        if ("DISPATCHED".equals(assignment.getStatus()) && !"RENTED".equals(device.getStatus())) {
            return "DISPATCHED_DEVICE_STATE_MISMATCH";
        }
        if ("RETURNED".equals(assignment.getStatus())
                && (assignment.getReturnedAt() == null || assignment.getInspectionCompletedAt() == null
                || !Set.of("AVAILABLE", "MAINTENANCE").contains(device.getStatus()))) {
            return "RETURNED_INSPECTION_FACTS_INCOMPLETE";
        }
        return null;
    }

    private static String operationalReviewReason(RentalRemarkPlanChangeType type) {
        return switch (type) {
            case REPLACEMENT -> "REMARK_REPLACEMENT_REVIEW";
            case DAMAGE -> "REMARK_DAMAGE_REVIEW";
            case LOSS -> "REMARK_LOSS_REVIEW";
            case OVERDUE -> "REMARK_OVERDUE_REVIEW";
            case LOGISTICS_DELAY -> "REMARK_LOGISTICS_DELAY_REVIEW";
            default -> null;
        };
    }

    private record AssignmentLockResult(List<RentalDeviceAssignmentDO> assignments,
                                        Map<Long, RentalDeviceDO> devices,
                                        String reasonCode) {

        private static AssignmentLockResult success(List<RentalDeviceAssignmentDO> assignments,
                                                    Map<Long, RentalDeviceDO> devices) {
            return new AssignmentLockResult(assignments, devices, null);
        }

        private static AssignmentLockResult review(String reasonCode) {
            return new AssignmentLockResult(List.of(), Map.of(), reasonCode);
        }
    }

}
