package cn.iocoder.yudao.module.rental.service;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalOrderPreparationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Owns one local assignment transaction. Every writer of occupied schedules must lock the device row first.
 */
@Service
public class RentalDeviceAssignmentServiceImpl implements RentalDeviceAssignmentService {

    static final String DEVICE_STATUS_AVAILABLE = "AVAILABLE";
    static final String RENTAL_ORDER_STATUS_PENDING_ALLOCATION = "PENDING_ALLOCATION";
    static final String ASSIGNMENT_STATUS_ASSIGNED = "ASSIGNED";
    static final String SCHEDULE_STATUS_EFFECTIVE = "EFFECTIVE";
    static final String SCHEDULE_TYPE_RENTAL = "RENTAL";

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalOrderItemMapper orderItemMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final RentalDeviceLockService deviceLockService;
    private final RentalOrderPreparationPolicy preparationPolicy;

    public RentalDeviceAssignmentServiceImpl(RentalDeviceAssignmentMapper assignmentMapper,
                                             RentalDeviceMapper deviceMapper,
                                             RentalOrderItemMapper orderItemMapper,
                                             RentalOrderMapper orderMapper,
                                             RentalScheduleMapper scheduleMapper,
                                             RentalDeviceLockService deviceLockService,
                                             RentalOrderPreparationPolicy preparationPolicy) {
        this.assignmentMapper = assignmentMapper;
        this.deviceMapper = deviceMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderMapper = orderMapper;
        this.scheduleMapper = scheduleMapper;
        this.deviceLockService = deviceLockService;
        this.preparationPolicy = preparationPolicy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentalDeviceAssignmentResult assign(RentalDeviceAssignmentCommand command) {
        validate(command);

        RentalDeviceAssignmentDO replay = assignmentMapper.selectByIdempotencyKeyForUpdate(command.idempotencyKey());
        if (replay != null) {
            return replayResult(replay, command);
        }

        RentalOrderItemDO snapshot = orderItemMapper.selectById(command.rentalOrderItemId());
        if (snapshot == null || snapshot.getRentalOrderId() == null) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.ORDER_ITEM_NOT_FOUND,
                    "Rental order item does not exist");
        }
        RentalOrderDO order = orderMapper.selectByIdForUpdate(snapshot.getRentalOrderId());
        RentalOrderItemDO item = orderItemMapper.selectByIdForUpdate(command.rentalOrderItemId());
        if (item == null || !Objects.equals(snapshot.getRentalOrderId(), item.getRentalOrderId())) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.ORDER_ITEM_NOT_FOUND,
                    "Rental order item changed while acquiring assignment locks");
        }
        requireOrderEligible(order, item);
        // Keep the global order -> item -> device -> schedule lock order used by remark updates.
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(command.deviceId());
        requireDeviceAssignable(device);
        if (!deviceLockService.getActiveLocksForUpdate(device.getId()).isEmpty()) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.DEVICE_LOCKED,
                    "Rental device has an active classified lock");
        }
        requireItemMatchesDevice(item, device);
        if (assignmentMapper.countAssignedByOrderItem(item.getId()) >= item.getQuantity()) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.ITEM_ALREADY_FULLY_ASSIGNED,
                    "Rental order item is already fully assigned");
        }

        List<RentalScheduleDO> conflicts = scheduleMapper.selectEffectiveOverlapsForUpdate(device.getId(),
                command.occupyStartDate(), command.occupyEndDateExclusive());
        if (conflicts.stream().anyMatch(schedule -> overlaps(command.occupyStartDate(), command.occupyEndDateExclusive(),
                schedule.getOccupyStartDate(), schedule.getOccupyEndDateExclusive()))) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.SCHEDULE_CONFLICT,
                    "Device has an overlapping occupied schedule");
        }

        RentalScheduleDO schedule = RentalScheduleDO.builder()
                .deviceId(device.getId())
                .rentalOrderId(order.getId())
                .rentalOrderItemId(item.getId())
                .scheduleType(SCHEDULE_TYPE_RENTAL)
                .status(SCHEDULE_STATUS_EFFECTIVE)
                .occupyStartDate(command.occupyStartDate())
                .occupyEndDateExclusive(command.occupyEndDateExclusive())
                .idempotencyKey(command.idempotencyKey())
                .build();
        scheduleMapper.insert(schedule);

        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .rentalOrderId(order.getId())
                .rentalOrderItemId(item.getId())
                .deviceId(device.getId())
                .scheduleId(schedule.getId())
                .status(ASSIGNMENT_STATUS_ASSIGNED)
                .idempotencyKey(command.idempotencyKey())
                .assignedAt(LocalDateTime.now(BUSINESS_ZONE))
                .build();
        assignmentMapper.insert(assignment);
        return result(assignment.getId(), schedule.getId(), device.getId(), command);
    }

    static boolean overlaps(LocalDate newStart, LocalDate newEndExclusive, LocalDate existingStart,
                            LocalDate existingEndExclusive) {
        return newStart.isBefore(existingEndExclusive) && newEndExclusive.isAfter(existingStart);
    }

    private RentalDeviceAssignmentResult result(Long assignmentId, Long scheduleId, Long deviceId,
                                                 RentalDeviceAssignmentCommand command) {
        return new RentalDeviceAssignmentResult(assignmentId, scheduleId, deviceId, command.occupyStartDate(),
                command.occupyEndDateExclusive());
    }

    private RentalDeviceAssignmentResult replayResult(RentalDeviceAssignmentDO replay,
                                                       RentalDeviceAssignmentCommand command) {
        RentalScheduleDO schedule = scheduleMapper.selectById(replay.getScheduleId());
        if (schedule == null || !Objects.equals(replay.getDeviceId(), command.deviceId())
                || !Objects.equals(schedule.getRentalOrderItemId(), command.rentalOrderItemId())
                || !Objects.equals(schedule.getOccupyStartDate(), command.occupyStartDate())
                || !Objects.equals(schedule.getOccupyEndDateExclusive(), command.occupyEndDateExclusive())) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency key is already bound to a different assignment");
        }
        return new RentalDeviceAssignmentResult(replay.getId(), replay.getScheduleId(), replay.getDeviceId(),
                schedule.getOccupyStartDate(), schedule.getOccupyEndDateExclusive());
    }

    private void validate(RentalDeviceAssignmentCommand command) {
        if (command == null || command.rentalOrderItemId() == null || command.deviceId() == null
                || command.occupyStartDate() == null || command.occupyEndDateExclusive() == null
                || !command.occupyStartDate().isBefore(command.occupyEndDateExclusive())
                || !StringUtils.hasText(command.idempotencyKey()) || command.idempotencyKey().length() > 128) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.INVALID_COMMAND,
                    "Assignment command is incomplete or has an invalid occupied period");
        }
    }

    private void requireDeviceAssignable(RentalDeviceDO device) {
        if (device == null) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.DEVICE_NOT_FOUND,
                    "Rental device does not exist");
        }
        if (!Boolean.TRUE.equals(device.getEnabled()) || !DEVICE_STATUS_AVAILABLE.equals(device.getStatus())) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.DEVICE_NOT_ASSIGNABLE,
                    "Rental device is not available for assignment");
        }
    }

    private void requireItemMatchesDevice(RentalOrderItemDO item, RentalDeviceDO device) {
        if (item == null) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.ORDER_ITEM_NOT_FOUND,
                    "Rental order item does not exist");
        }
        if (item.getQuantity() == null || item.getQuantity() < 1
                || !Objects.equals(item.getEquipmentModelCode(), device.getEquipmentModelCode())) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.DEVICE_MODEL_MISMATCH,
                    "Rental device does not match the requested equipment model");
        }
    }

    private void requireOrderEligible(RentalOrderDO order, RentalOrderItemDO item) {
        if (order == null || !Objects.equals(order.getId(), item.getRentalOrderId())
                || !RENTAL_ORDER_STATUS_PENDING_ALLOCATION.equals(order.getStatus())) {
            throw new RentalDeviceAssignmentException(RentalDeviceAssignmentException.Code.ORDER_NOT_ELIGIBLE,
                    "Rental order is not eligible for allocation");
        }
        preparationPolicy.requireReady(order, item);
    }

}
