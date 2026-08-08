package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCandidateRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCandidatesRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceScheduleDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalOrderScheduleDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalPendingAllocationItemRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalPendingAllocationOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalPendingAllocationPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleAssignmentRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleDeliveryRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleLockRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleOrderItemRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleSegmentRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleAllocationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;

/**
 * Read model for pending allocation and the schedule V2 decision drawers.
 *
 * <p>This service never writes schedules, assignments, locks, or logistics
 * state. Final assignment remains owned by the existing transactional service.</p>
 */
@Service
public class RentalScheduleAllocationService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> ACTIVE_ASSIGNMENT_STATUSES = Set.of("ASSIGNED", "DISPATCHED");
    private static final Set<String> TERMINAL_TRACKING_STATUSES = Set.of("DELIVERED", "RETURNED");
    private static final int MAX_CANDIDATE_DEVICES = 100;
    private static final int MAX_NEIGHBORING_SCHEDULES = 8;
    private static final long STALE_HOURS = 24;

    private final RentalScheduleAllocationMapper allocationMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalOrderItemMapper orderItemMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryDeviceRelMapper deliveryRelationMapper;

    public RentalScheduleAllocationService(RentalScheduleAllocationMapper allocationMapper,
                                           RentalOrderMapper orderMapper,
                                           RentalOrderItemMapper orderItemMapper,
                                           RentalDeviceMapper deviceMapper,
                                           RentalDeviceAssignmentMapper assignmentMapper,
                                           RentalScheduleMapper scheduleMapper,
                                           RentalDeliveryMapper deliveryMapper,
                                           RentalDeliveryDeviceRelMapper deliveryRelationMapper) {
        this.allocationMapper = allocationMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.deviceMapper = deviceMapper;
        this.assignmentMapper = assignmentMapper;
        this.scheduleMapper = scheduleMapper;
        this.deliveryMapper = deliveryMapper;
        this.deliveryRelationMapper = deliveryRelationMapper;
    }

    public PageResult<RentalPendingAllocationOrderRespVO> getPendingAllocationPage(
            RentalPendingAllocationPageReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String orderNo = trimToNull(reqVO.getOrderNo());
        String modelCode = trimToNull(reqVO.getEquipmentModelCode());
        long total = safeLong(allocationMapper.countPendingAllocationOrders(tenantId, orderNo, modelCode));
        if (total == 0) {
            return new PageResult<>(List.of(), 0L);
        }

        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        long offset = Math.multiplyExact((long) Math.max(pageNo - 1, 0), pageSize);
        List<RentalOrderDO> orders = tenantRows(
                allocationMapper.selectPendingAllocationOrders(tenantId, orderNo, modelCode, offset, pageSize),
                tenantId);
        if (orders.isEmpty()) {
            return new PageResult<>(List.of(), total);
        }

        List<Long> orderIds = ids(orders, RentalOrderDO::getId);
        List<RentalOrderItemDO> items = tenantRows(orderItemMapper.selectListByRentalOrderIds(orderIds), tenantId);
        List<RentalDeviceAssignmentDO> assignments = tenantRows(
                assignmentMapper.selectActiveListByRentalOrderIds(orderIds), tenantId);
        Map<Long, List<RentalDeviceAssignmentDO>> assignmentsByItem =
                group(assignments, RentalDeviceAssignmentDO::getRentalOrderItemId);
        Map<Long, List<RentalOrderItemDO>> itemsByOrder = group(items, RentalOrderItemDO::getRentalOrderId);

        List<RentalPendingAllocationOrderRespVO> result = orders.stream()
                .map(order -> toPendingOrder(order, itemsByOrder.getOrDefault(order.getId(), List.of()),
                        assignmentsByItem))
                .filter(order -> !order.getItems().isEmpty())
                .toList();
        return new PageResult<>(result, total);
    }

    public RentalDeviceCandidatesRespVO getDeviceCandidates(Long rentalOrderItemId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalOrderItemDO item = tenantRow(orderItemMapper.selectById(rentalOrderItemId), tenantId);
        if (item == null) {
            return null;
        }
        RentalOrderDO order = tenantRow(orderMapper.selectById(item.getRentalOrderId()), tenantId);
        RentalDeviceCandidatesRespVO result = new RentalDeviceCandidatesRespVO();
        result.setRentalOrderId(item.getRentalOrderId());
        result.setRentalOrderItemId(item.getId());
        result.setOrderNo(order == null ? null : order.getOrderNo());
        result.setEquipmentModelCode(item.getEquipmentModelCode());
        result.setRequiredQuantity(quantity(item));
        int assignedQuantity = countActiveAssignments(
                tenantRows(assignmentMapper.selectList(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                        .eq(RentalDeviceAssignmentDO::getRentalOrderItemId, item.getId())
                        .in(RentalDeviceAssignmentDO::getStatus, ACTIVE_ASSIGNMENT_STATUSES)
                        .orderByAsc(RentalDeviceAssignmentDO::getId)), tenantId));
        result.setAssignedQuantity(assignedQuantity);
        result.setRemainingQuantity(Math.max(0, quantity(item) - assignedQuantity));
        result.setOccupyStartDate(item.getOccupyStartDate());
        result.setOccupyEndDateExclusive(item.getOccupyEndDateExclusive());

        if (result.getRemainingQuantity() == 0) {
            result.setReasonCodes(List.of("ITEM_FULLY_ASSIGNED"));
            result.setCandidates(List.of());
            return result;
        }
        if (order == null || !"PENDING_ALLOCATION".equals(order.getStatus())) {
            result.setReasonCodes(List.of("ORDER_NOT_ELIGIBLE"));
        }

        List<RentalDeviceDO> devices = tenantRows(deviceMapper.selectList(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getEquipmentModelCode, item.getEquipmentModelCode())
                .orderByAsc(RentalDeviceDO::getId)
                .last("LIMIT " + MAX_CANDIDATE_DEVICES)), tenantId);
        List<Long> deviceIds = ids(devices, RentalDeviceDO::getId);
        if (deviceIds.isEmpty()) {
            result.setCandidates(List.of());
            return result;
        }

        List<RentalScheduleDO> schedules = tenantRows(scheduleMapper.selectList(
                new LambdaQueryWrapper<RentalScheduleDO>()
                        .in(RentalScheduleDO::getDeviceId, deviceIds)
                        .eq(RentalScheduleDO::getStatus, "EFFECTIVE")
                        .orderByAsc(RentalScheduleDO::getOccupyStartDate)
                        .orderByAsc(RentalScheduleDO::getId)), tenantId);
        List<RentalDeviceLockDO> locks = tenantRows(
                allocationMapper.selectActiveLocks(tenantId, deviceIds, now()), tenantId);
        List<RentalDeliveryDeviceRelDO> relations = tenantRows(deliveryRelationMapper.selectList(
                new LambdaQueryWrapper<RentalDeliveryDeviceRelDO>().in(
                        RentalDeliveryDeviceRelDO::getDeviceId, deviceIds)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getDeviceId)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getId)), tenantId);
        Map<Long, RentalDeliveryDO> deliveriesById = deliveriesById(relations, tenantId);
        Map<Long, List<RentalScheduleDO>> schedulesByDevice = group(schedules, RentalScheduleDO::getDeviceId);
        Map<Long, List<RentalDeviceLockDO>> locksByDevice = group(locks, RentalDeviceLockDO::getDeviceId);
        Map<Long, List<RentalDeliveryDeviceRelDO>> relationsByDevice =
                group(relations, RentalDeliveryDeviceRelDO::getDeviceId);

        result.setCandidates(devices.stream()
                .map(device -> toCandidate(device, item, order,
                        schedulesByDevice.getOrDefault(device.getId(), List.of()),
                        locksByDevice.getOrDefault(device.getId(), List.of()),
                        relationsByDevice.getOrDefault(device.getId(), List.of()),
                        deliveriesById))
                .toList());
        return result;
    }

    public RentalOrderScheduleDetailRespVO getOrderScheduleDetail(Long rentalOrderId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalOrderDO order = tenantRow(orderMapper.selectById(rentalOrderId), tenantId);
        if (order == null) {
            return null;
        }

        List<RentalOrderItemDO> items = tenantRows(orderItemMapper.selectListByRentalOrderIds(
                List.of(rentalOrderId)), tenantId);
        List<RentalDeviceAssignmentDO> assignments = tenantRows(assignmentMapper.selectList(
                new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                        .eq(RentalDeviceAssignmentDO::getRentalOrderId, rentalOrderId)
                        .orderByAsc(RentalDeviceAssignmentDO::getId)), tenantId);
        List<RentalScheduleDO> schedules = tenantRows(scheduleMapper.selectList(
                new LambdaQueryWrapper<RentalScheduleDO>()
                        .eq(RentalScheduleDO::getRentalOrderId, rentalOrderId)
                        .orderByAsc(RentalScheduleDO::getOccupyStartDate)
                        .orderByAsc(RentalScheduleDO::getId)), tenantId);
        List<RentalDeliveryDO> deliveries = tenantRows(deliveryMapper.selectList(
                new LambdaQueryWrapper<RentalDeliveryDO>()
                        .eq(RentalDeliveryDO::getRentalOrderId, rentalOrderId)
                        .orderByAsc(RentalDeliveryDO::getDirection)
                        .orderByAsc(RentalDeliveryDO::getPackageSeq)
                        .orderByAsc(RentalDeliveryDO::getId)), tenantId);
        List<RentalDeliveryDeviceRelDO> relations = tenantRows(deliveryRelationMapper.selectList(
                new LambdaQueryWrapper<RentalDeliveryDeviceRelDO>()
                        .eq(RentalDeliveryDeviceRelDO::getRentalOrderId, rentalOrderId)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getDeliveryId)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getId)), tenantId);
        Map<Long, RentalDeviceDO> devicesById = devicesById(assignments, schedules, tenantId);
        Map<Long, RentalScheduleDO> schedulesById = indexById(schedules, RentalScheduleDO::getId);
        Map<Long, List<RentalDeviceAssignmentDO>> assignmentsByItem =
                group(assignments, RentalDeviceAssignmentDO::getRentalOrderItemId);
        Map<Long, List<Long>> deviceIdsByDelivery = relationDeviceIds(relations);

        RentalOrderScheduleDetailRespVO result = new RentalOrderScheduleDetailRespVO();
        copyOrder(result, order);
        List<RentalScheduleOrderItemRespVO> itemResults = items.stream()
                .map(item -> toOrderItem(item, assignmentsByItem.getOrDefault(item.getId(), List.of()),
                        devicesById, schedulesById, tenantId))
                .toList();
        result.setItems(itemResults);
        result.setRequiredQuantity(itemResults.stream().mapToInt(item -> required(item)).sum());
        result.setAssignedQuantity(itemResults.stream().mapToInt(item -> assigned(item)).sum());
        result.setRemainingQuantity(itemResults.stream().mapToInt(item -> remaining(item)).sum());
        result.setDeliveries(toDeliveryResponses(deliveries, deviceIdsByDelivery));
        result.setRiskCodes(logisticsRiskCodes(deliveries, now()));
        return result;
    }

    public RentalDeviceScheduleDetailRespVO getDeviceScheduleDetail(Long deviceId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeviceDO device = tenantRow(deviceMapper.selectById(deviceId), tenantId);
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        List<RentalScheduleDO> schedules = tenantRows(scheduleMapper.selectList(
                new LambdaQueryWrapper<RentalScheduleDO>()
                        .eq(RentalScheduleDO::getDeviceId, deviceId)
                        .eq(RentalScheduleDO::getStatus, "EFFECTIVE")
                        .orderByAsc(RentalScheduleDO::getOccupyStartDate)
                        .orderByAsc(RentalScheduleDO::getId)), tenantId);
        List<RentalDeviceAssignmentDO> assignments = tenantRows(assignmentMapper.selectList(
                new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                        .eq(RentalDeviceAssignmentDO::getDeviceId, deviceId)
                        .orderByDesc(RentalDeviceAssignmentDO::getId)), tenantId);
        List<RentalDeliveryDeviceRelDO> relations = tenantRows(deliveryRelationMapper.selectList(
                new LambdaQueryWrapper<RentalDeliveryDeviceRelDO>()
                        .eq(RentalDeliveryDeviceRelDO::getDeviceId, deviceId)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getDeliveryId)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getId)), tenantId);
        Map<Long, RentalDeliveryDO> deliveriesById = deliveriesById(relations, tenantId);
        List<RentalDeviceLockDO> locks = tenantRows(
                allocationMapper.selectActiveLocks(tenantId, List.of(deviceId), now()), tenantId);
        Map<Long, RentalDeviceDO> deviceMap = Map.of(deviceId, device);
        Map<Long, RentalScheduleDO> schedulesById = indexById(schedules, RentalScheduleDO::getId);
        RentalDeviceAssignmentDO current = assignments.stream()
                .filter(assignment -> ACTIVE_ASSIGNMENT_STATUSES.contains(assignment.getStatus()))
                .findFirst().orElse(null);

        RentalDeviceScheduleDetailRespVO result = new RentalDeviceScheduleDetailRespVO();
        result.setId(device.getId());
        result.setDeviceNo(device.getDeviceNo());
        result.setSerialNumber(device.getSerialNumber());
        result.setEquipmentModelCode(device.getEquipmentModelCode());
        result.setStatus(device.getStatus());
        result.setEnabled(device.getEnabled());
        result.setActiveLocks(toLockResponses(locks));
        result.setSchedules(toScheduleResponses(schedules, Map.of()));
        result.setCurrentAssignment(current == null ? null
                : toAssignment(current, deviceMap, schedulesById));
        result.setDeliveries(toDeliveryResponses(new java.util.ArrayList<>(deliveriesById.values()),
                relationDeviceIds(relations)));
        result.setExpectedReleaseDate(expectedReleaseDate(schedules, locks));
        result.setReasonCodes(deviceReasonCodes(device, locks, deliveriesById.values()));
        result.setInspectionState(inspectionState(device, locks));
        result.setMaintenanceState(maintenanceState(device, locks));
        return result;
    }

    private RentalPendingAllocationOrderRespVO toPendingOrder(
            RentalOrderDO order, List<RentalOrderItemDO> items,
            Map<Long, List<RentalDeviceAssignmentDO>> assignmentsByItem) {
        RentalPendingAllocationOrderRespVO result = new RentalPendingAllocationOrderRespVO();
        result.setId(order.getId());
        result.setOrderNo(order.getOrderNo());
        result.setSourceType(order.getSourceType());
        result.setSourceOrderId(order.getSourceOrderId());
        result.setStatus(order.getStatus());
        result.setRentAmount(order.getRentAmount());
        result.setRefundAmount(order.getRefundAmount());
        result.setBillableStartDate(order.getBillableStartDate());
        result.setBillableEndDate(order.getBillableEndDate());
        result.setOccupyStartDate(order.getOccupyStartDate());
        result.setOccupyEndDateExclusive(order.getOccupyEndDateExclusive());
        List<RentalPendingAllocationItemRespVO> itemResults = items.stream()
                .map(item -> toPendingItem(item, assignmentsByItem.getOrDefault(item.getId(), List.of())))
                .filter(item -> item.getRemainingQuantity() > 0)
                .toList();
        result.setItems(itemResults);
        result.setRequiredQuantity(itemResults.stream().mapToInt(RentalPendingAllocationItemRespVO::getRequiredQuantity).sum());
        result.setAssignedQuantity(itemResults.stream().mapToInt(RentalPendingAllocationItemRespVO::getAssignedQuantity).sum());
        result.setRemainingQuantity(itemResults.stream().mapToInt(RentalPendingAllocationItemRespVO::getRemainingQuantity).sum());
        return result;
    }

    private RentalPendingAllocationItemRespVO toPendingItem(
            RentalOrderItemDO item, List<RentalDeviceAssignmentDO> assignments) {
        RentalPendingAllocationItemRespVO result = new RentalPendingAllocationItemRespVO();
        result.setId(item.getId());
        result.setRentalOrderId(item.getRentalOrderId());
        result.setEquipmentModelCode(item.getEquipmentModelCode());
        result.setRequiredQuantity(quantity(item));
        result.setAssignedQuantity(assignments.size());
        result.setRemainingQuantity(Math.max(0, quantity(item) - assignments.size()));
        result.setRentAmount(item.getRentAmount());
        result.setBillableStartDate(item.getBillableStartDate());
        result.setBillableEndDate(item.getBillableEndDate());
        result.setOccupyStartDate(item.getOccupyStartDate());
        result.setOccupyEndDateExclusive(item.getOccupyEndDateExclusive());
        return result;
    }

    private RentalDeviceCandidateRespVO toCandidate(
            RentalDeviceDO device, RentalOrderItemDO item, RentalOrderDO order,
            List<RentalScheduleDO> schedules, List<RentalDeviceLockDO> locks,
            List<RentalDeliveryDeviceRelDO> relations, Map<Long, RentalDeliveryDO> deliveriesById) {
        LocalDateTime currentTime = now();
        List<RentalDeliveryDO> deliveries = relations.stream()
                .map(RentalDeliveryDeviceRelDO::getDeliveryId)
                .map(deliveriesById::get)
                .filter(Objects::nonNull)
                .toList();
        List<String> reasons = new java.util.ArrayList<>();
        if (!Boolean.TRUE.equals(device.getEnabled())) {
            reasons.add("DEVICE_DISABLED");
        }
        if (!"AVAILABLE".equals(device.getStatus())) {
            reasons.add("DEVICE_STATUS_NOT_AVAILABLE");
        }
        if (order == null || !"PENDING_ALLOCATION".equals(order.getStatus())) {
            reasons.add("ORDER_NOT_ELIGIBLE");
        }
        if (!validPeriod(item.getOccupyStartDate(), item.getOccupyEndDateExclusive())) {
            reasons.add("OCCUPIED_PERIOD_INVALID");
        } else if (schedules.stream().anyMatch(schedule -> overlaps(
                item.getOccupyStartDate(), item.getOccupyEndDateExclusive(),
                schedule.getOccupyStartDate(), schedule.getOccupyEndDateExclusive()))) {
            reasons.add("SCHEDULE_CONFLICT");
        }
        if (!locks.isEmpty()) {
            reasons.add("DEVICE_LOCKED");
            locks.stream().map(RentalDeviceLockDO::getLockType).distinct().sorted()
                    .map(this::lockReasonCode).forEach(reasons::add);
        }
        List<String> deliveryRiskCodes = logisticsRiskCodes(deliveries, currentTime);
        if (!deliveryRiskCodes.isEmpty()) {
            reasons.add("LOGISTICS_RISK");
        }
        boolean eligible = reasons.isEmpty();
        if (eligible) {
            reasons.add("ELIGIBLE");
        }

        RentalDeviceCandidateRespVO result = new RentalDeviceCandidateRespVO();
        result.setId(device.getId());
        result.setDeviceNo(device.getDeviceNo());
        result.setSerialNumber(device.getSerialNumber());
        result.setEquipmentModelCode(device.getEquipmentModelCode());
        result.setStatus(device.getStatus());
        result.setEnabled(device.getEnabled());
        result.setEligible(eligible);
        result.setReasonCodes(List.copyOf(reasons));
        result.setNextAvailableDate(eligible ? item.getOccupyStartDate()
                : nextAvailableDate(device, item, schedules, locks));
        result.setNeighboringSchedules(toScheduleResponses(
                schedules.stream().sorted(scheduleComparator()).limit(MAX_NEIGHBORING_SCHEDULES).toList(), Map.of()));
        result.setActiveLocks(toLockResponses(locks));
        result.setLogistics(toDeliveryResponses(deliveries, Map.of()));
        return result;
    }

    private RentalScheduleOrderItemRespVO toOrderItem(
            RentalOrderItemDO item, List<RentalDeviceAssignmentDO> assignments,
            Map<Long, RentalDeviceDO> devicesById, Map<Long, RentalScheduleDO> schedulesById,
            Long tenantId) {
        List<RentalDeviceAssignmentDO> activeAssignments = assignments.stream()
                .filter(assignment -> ACTIVE_ASSIGNMENT_STATUSES.contains(assignment.getStatus()))
                .toList();
        RentalScheduleOrderItemRespVO result = new RentalScheduleOrderItemRespVO();
        result.setId(item.getId());
        result.setRentalOrderId(item.getRentalOrderId());
        result.setEquipmentModelCode(item.getEquipmentModelCode());
        result.setRequiredQuantity(quantity(item));
        result.setAssignedQuantity(activeAssignments.size());
        result.setRemainingQuantity(Math.max(0, quantity(item) - activeAssignments.size()));
        result.setRentAmount(item.getRentAmount());
        result.setBillableStartDate(item.getBillableStartDate());
        result.setBillableEndDate(item.getBillableEndDate());
        result.setOccupyStartDate(item.getOccupyStartDate());
        result.setOccupyEndDateExclusive(item.getOccupyEndDateExclusive());
        result.setAssignments(assignments.stream()
                .map(assignment -> toAssignment(assignment, devicesById, schedulesById))
                .toList());
        return result;
    }

    private RentalScheduleAssignmentRespVO toAssignment(
            RentalDeviceAssignmentDO assignment, Map<Long, RentalDeviceDO> devicesById,
            Map<Long, RentalScheduleDO> schedulesById) {
        RentalDeviceDO device = devicesById.get(assignment.getDeviceId());
        RentalScheduleDO schedule = schedulesById.get(assignment.getScheduleId());
        RentalScheduleAssignmentRespVO result = new RentalScheduleAssignmentRespVO();
        result.setId(assignment.getId());
        result.setRentalOrderId(assignment.getRentalOrderId());
        result.setRentalOrderItemId(assignment.getRentalOrderItemId());
        result.setDeviceId(assignment.getDeviceId());
        if (device != null) {
            result.setDeviceNo(device.getDeviceNo());
            result.setSerialNumber(device.getSerialNumber());
            result.setDeviceStatus(device.getStatus());
            result.setDeviceEnabled(device.getEnabled());
        }
        result.setStatus(assignment.getStatus());
        result.setScheduleId(assignment.getScheduleId());
        if (schedule != null) {
            result.setScheduleStatus(schedule.getStatus());
            result.setOccupyStartDate(schedule.getOccupyStartDate());
            result.setOccupyEndDateExclusive(schedule.getOccupyEndDateExclusive());
        }
        result.setAssignedAt(assignment.getAssignedAt());
        return result;
    }

    private List<RentalScheduleSegmentRespVO> toScheduleResponses(
            Collection<RentalScheduleDO> schedules, Map<Long, RentalOrderDO> ordersById) {
        return schedules.stream().map(schedule -> {
            RentalScheduleSegmentRespVO result = new RentalScheduleSegmentRespVO();
            result.setId(schedule.getId());
            result.setDeviceId(schedule.getDeviceId());
            result.setRentalOrderId(schedule.getRentalOrderId());
            result.setRentalOrderItemId(schedule.getRentalOrderItemId());
            RentalOrderDO order = ordersById.get(schedule.getRentalOrderId());
            result.setOrderNo(order == null ? null : order.getOrderNo());
            result.setScheduleType(schedule.getScheduleType());
            result.setStatus(schedule.getStatus());
            result.setOccupyStartDate(schedule.getOccupyStartDate());
            result.setOccupyEndDateExclusive(schedule.getOccupyEndDateExclusive());
            return result;
        }).toList();
    }

    private List<RentalScheduleDeliveryRespVO> toDeliveryResponses(
            Collection<RentalDeliveryDO> deliveries, Map<Long, List<Long>> deviceIdsByDelivery) {
        LocalDateTime currentTime = now();
        return deliveries.stream().sorted(Comparator.comparing(RentalDeliveryDO::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(delivery -> toDelivery(delivery,
                        deviceIdsByDelivery.getOrDefault(delivery.getId(), List.of()), currentTime))
                .toList();
    }

    private RentalScheduleDeliveryRespVO toDelivery(
            RentalDeliveryDO delivery, List<Long> deviceIds, LocalDateTime currentTime) {
        RentalScheduleDeliveryRespVO result = new RentalScheduleDeliveryRespVO();
        result.setId(delivery.getId());
        result.setRentalOrderId(delivery.getRentalOrderId());
        result.setDirection(delivery.getDirection());
        result.setPackageSeq(delivery.getPackageSeq());
        result.setSourceCarrierName(delivery.getSourceCarrierName());
        result.setTrackingStatus(delivery.getTrackingStatus());
        result.setMappingStatus(delivery.getMappingStatus());
        result.setSubscribeStatus(delivery.getSubscribeStatus());
        result.setQueryStatus(delivery.getQueryStatus());
        result.setLatestEventTime(delivery.getLatestEventTime());
        result.setLastSyncedAt(delivery.getLastSyncedAt());
        result.setEstimatedDeliveryAt(delivery.getEstimatedDeliveryAt());
        result.setStale(isStale(delivery, currentTime));
        result.setDeviceIds(List.copyOf(new LinkedHashSet<>(deviceIds)));
        return result;
    }

    private List<RentalScheduleLockRespVO> toLockResponses(Collection<RentalDeviceLockDO> locks) {
        return locks.stream().sorted(Comparator.comparing(RentalDeviceLockDO::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(lock -> {
                    RentalScheduleLockRespVO result = new RentalScheduleLockRespVO();
                    result.setId(lock.getId());
                    result.setDeviceId(lock.getDeviceId());
                    result.setLockType(lock.getLockType());
                    result.setReason(lock.getReason());
                    result.setRentalOrderId(lock.getRentalOrderId());
                    result.setRentalOrderItemId(lock.getRentalOrderItemId());
                    result.setSourceType(lock.getSourceType());
                    result.setStartTime(lock.getStartTime());
                    result.setPlannedEndTime(lock.getPlannedEndTime());
                    result.setStatus(lock.getStatus());
                    return result;
                }).toList();
    }

    private Map<Long, RentalDeviceDO> devicesById(
            List<RentalDeviceAssignmentDO> assignments, List<RentalScheduleDO> schedules, Long tenantId) {
        Set<Long> deviceIds = new LinkedHashSet<>();
        assignments.stream().map(RentalDeviceAssignmentDO::getDeviceId).filter(Objects::nonNull).forEach(deviceIds::add);
        schedules.stream().map(RentalScheduleDO::getDeviceId).filter(Objects::nonNull).forEach(deviceIds::add);
        return deviceIds.isEmpty() ? Map.of()
                : indexById(tenantRows(deviceMapper.selectByIds(deviceIds), tenantId), RentalDeviceDO::getId);
    }

    private Map<Long, RentalDeliveryDO> deliveriesById(
            List<RentalDeliveryDeviceRelDO> relations, Long tenantId) {
        List<Long> deliveryIds = relations.stream().map(RentalDeliveryDeviceRelDO::getDeliveryId)
                .filter(Objects::nonNull).distinct().toList();
        return deliveryIds.isEmpty() ? Map.of()
                : indexById(tenantRows(deliveryMapper.selectByIds(deliveryIds), tenantId), RentalDeliveryDO::getId);
    }

    private Map<Long, List<Long>> relationDeviceIds(List<RentalDeliveryDeviceRelDO> relations) {
        return relations.stream().filter(relation -> relation.getDeliveryId() != null && relation.getDeviceId() != null)
                .collect(Collectors.groupingBy(RentalDeliveryDeviceRelDO::getDeliveryId, LinkedHashMap::new,
                        Collectors.mapping(RentalDeliveryDeviceRelDO::getDeviceId, Collectors.toList())));
    }

    private List<String> deviceReasonCodes(
            RentalDeviceDO device, List<RentalDeviceLockDO> locks, Collection<RentalDeliveryDO> deliveries) {
        List<String> reasons = new java.util.ArrayList<>();
        if (!Boolean.TRUE.equals(device.getEnabled())) {
            reasons.add("DEVICE_DISABLED");
        }
        if (!"AVAILABLE".equals(device.getStatus())) {
            reasons.add("DEVICE_STATUS_NOT_AVAILABLE");
        }
        if (!locks.isEmpty()) {
            reasons.add("DEVICE_LOCKED");
            locks.stream().map(RentalDeviceLockDO::getLockType).distinct().sorted()
                    .map(this::lockReasonCode).forEach(reasons::add);
        }
        if (!logisticsRiskCodes(deliveries, now()).isEmpty()) {
            reasons.add("LOGISTICS_RISK");
        }
        return List.copyOf(reasons);
    }

    private String inspectionState(RentalDeviceDO device, List<RentalDeviceLockDO> locks) {
        if (locks.stream().anyMatch(lock -> "RETURN_INSPECTION".equals(lock.getLockType()))) {
            return "PENDING";
        }
        if ("MAINTENANCE".equals(device.getStatus())) {
            return "FAILED";
        }
        return "NOT_RECORDED";
    }

    private String maintenanceState(RentalDeviceDO device, List<RentalDeviceLockDO> locks) {
        return "MAINTENANCE".equals(device.getStatus())
                || locks.stream().anyMatch(lock -> "MAINTENANCE".equals(lock.getLockType()))
                ? "ISOLATED" : "NONE";
    }

    private LocalDate expectedReleaseDate(
            List<RentalScheduleDO> schedules, List<RentalDeviceLockDO> locks) {
        LocalDate latest = schedules.stream().map(RentalScheduleDO::getOccupyEndDateExclusive)
                .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        for (RentalDeviceLockDO lock : locks) {
            if (lock.getPlannedEndTime() != null) {
                LocalDate lockEnd = lock.getPlannedEndTime().toLocalDate();
                if (latest == null || lockEnd.isAfter(latest)) {
                    latest = lockEnd;
                }
            }
        }
        return latest;
    }

    private LocalDate nextAvailableDate(
            RentalDeviceDO device, RentalOrderItemDO item,
            List<RentalScheduleDO> schedules, List<RentalDeviceLockDO> locks) {
        if (!Boolean.TRUE.equals(device.getEnabled()) || !"AVAILABLE".equals(device.getStatus())
                || !validPeriod(item.getOccupyStartDate(), item.getOccupyEndDateExclusive())) {
            return null;
        }
        long duration = ChronoUnit.DAYS.between(item.getOccupyStartDate(), item.getOccupyEndDateExclusive());
        LocalDate cursor = item.getOccupyStartDate();
        for (int attempt = 0; attempt < 32; attempt++) {
            LocalDate candidateStart = cursor;
            LocalDate candidateEnd = candidateStart.plusDays(duration);
            LocalDate scheduleEnd = schedules.stream()
                    .filter(schedule -> overlaps(candidateStart, candidateEnd,
                            schedule.getOccupyStartDate(), schedule.getOccupyEndDateExclusive()))
                    .map(RentalScheduleDO::getOccupyEndDateExclusive)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo).orElse(null);
            LocalDate lockEnd = locks.stream().map(RentalDeviceLockDO::getPlannedEndTime)
                    .filter(Objects::nonNull).map(LocalDateTime::toLocalDate)
                    .max(LocalDate::compareTo).orElse(null);
            LocalDate next = max(candidateStart, scheduleEnd, lockEnd);
            if (next.equals(candidateStart)) {
                return candidateStart;
            }
            cursor = next;
        }
        return null;
    }

    private List<String> logisticsRiskCodes(Collection<RentalDeliveryDO> deliveries, LocalDateTime currentTime) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (RentalDeliveryDO delivery : deliveries) {
            if ("EXCEPTION".equals(delivery.getTrackingStatus())) {
                result.add("LOGISTICS_EXCEPTION");
            }
            if (!TERMINAL_TRACKING_STATUSES.contains(delivery.getTrackingStatus())
                    && isStale(delivery, currentTime)) {
                result.add("TRACKING_STALE");
            }
            if ("RETURN".equals(delivery.getDirection())
                    && !TERMINAL_TRACKING_STATUSES.contains(delivery.getTrackingStatus())) {
                result.add("RETURN_IN_TRANSIT");
            }
        }
        return List.copyOf(result);
    }

    private boolean isStale(RentalDeliveryDO delivery, LocalDateTime currentTime) {
        return delivery.getLastSyncedAt() != null
                && delivery.getLastSyncedAt().isBefore(currentTime.minusHours(STALE_HOURS))
                && !TERMINAL_TRACKING_STATUSES.contains(delivery.getTrackingStatus());
    }

    private String lockReasonCode(String lockType) {
        return switch (lockType) {
            case "RETURN_INSPECTION" -> "RETURN_INSPECTION_PENDING";
            case "MAINTENANCE" -> "MAINTENANCE_LOCKED";
            case "ORDER_HOLD" -> "ORDER_HOLD";
            case "MANUAL_HOLD" -> "MANUAL_HOLD";
            default -> "UNKNOWN_DEVICE_LOCK";
        };
    }

    private static boolean overlaps(LocalDate start, LocalDate end, LocalDate existingStart, LocalDate existingEnd) {
        return validPeriod(start, end) && validPeriod(existingStart, existingEnd)
                && start.isBefore(existingEnd) && end.isAfter(existingStart);
    }

    private static boolean validPeriod(LocalDate start, LocalDate end) {
        return start != null && end != null && start.isBefore(end);
    }

    private static int quantity(RentalOrderItemDO item) {
        return item.getQuantity() == null ? 0 : Math.max(item.getQuantity(), 0);
    }

    private static int countActiveAssignments(List<RentalDeviceAssignmentDO> assignments) {
        return (int) assignments.stream()
                .filter(assignment -> ACTIVE_ASSIGNMENT_STATUSES.contains(assignment.getStatus())).count();
    }

    private static int required(RentalScheduleOrderItemRespVO item) {
        return item.getRequiredQuantity() == null ? 0 : item.getRequiredQuantity();
    }

    private static int assigned(RentalScheduleOrderItemRespVO item) {
        return item.getAssignedQuantity() == null ? 0 : item.getAssignedQuantity();
    }

    private static int remaining(RentalScheduleOrderItemRespVO item) {
        return item.getRemainingQuantity() == null ? 0 : item.getRemainingQuantity();
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static LocalDate max(LocalDate current, LocalDate... candidates) {
        LocalDate result = current;
        for (LocalDate candidate : candidates) {
            if (candidate != null && candidate.isAfter(result)) {
                result = candidate;
            }
        }
        return result;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static <T> T tenantRow(T row, Long tenantId) {
        if (row == null) {
            return null;
        }
        if (row instanceof RentalOrderDO value) {
            return Objects.equals(tenantId, value.getTenantId()) ? row : null;
        }
        if (row instanceof RentalOrderItemDO value) {
            return Objects.equals(tenantId, value.getTenantId()) ? row : null;
        }
        if (row instanceof RentalDeviceDO value) {
            return Objects.equals(tenantId, value.getTenantId()) ? row : null;
        }
        return row;
    }

    private static <T> List<T> tenantRows(Collection<T> rows, Long tenantId) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().filter(row -> belongsToTenant(row, tenantId)).toList();
    }

    private static boolean belongsToTenant(Object row, Long tenantId) {
        if (row instanceof RentalOrderDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        if (row instanceof RentalOrderItemDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        if (row instanceof RentalDeviceDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        if (row instanceof RentalDeviceAssignmentDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        if (row instanceof RentalScheduleDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        if (row instanceof RentalDeliveryDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        if (row instanceof RentalDeliveryDeviceRelDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        if (row instanceof RentalDeviceLockDO value) {
            return Objects.equals(tenantId, value.getTenantId());
        }
        return false;
    }

    private static <T, K> Map<K, List<T>> group(Collection<T> rows, Function<T, K> keyFunction) {
        return rows.stream().filter(Objects::nonNull).filter(row -> keyFunction.apply(row) != null)
                .collect(Collectors.groupingBy(keyFunction, LinkedHashMap::new, Collectors.toList()));
    }

    private static <T, K> Map<K, T> indexById(Collection<T> rows, Function<T, K> keyFunction) {
        return rows.stream().filter(Objects::nonNull).filter(row -> keyFunction.apply(row) != null)
                .collect(Collectors.toMap(keyFunction, Function.identity(), (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private static <T> List<Long> ids(Collection<T> rows, Function<T, Long> idFunction) {
        return rows.stream().map(idFunction).filter(Objects::nonNull).distinct().toList();
    }

    private static Comparator<RentalScheduleDO> scheduleComparator() {
        return Comparator.comparing(RentalScheduleDO::getOccupyStartDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RentalScheduleDO::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static void copyOrder(RentalOrderScheduleDetailRespVO target, RentalOrderDO source) {
        target.setId(source.getId());
        target.setOrderNo(source.getOrderNo());
        target.setSourceType(source.getSourceType());
        target.setSourceOrderId(source.getSourceOrderId());
        target.setStatus(source.getStatus());
        target.setRentAmount(source.getRentAmount());
        target.setRefundAmount(source.getRefundAmount());
        target.setBillableStartDate(source.getBillableStartDate());
        target.setBillableEndDate(source.getBillableEndDate());
        target.setOccupyStartDate(source.getOccupyStartDate());
        target.setOccupyEndDateExclusive(source.getOccupyEndDateExclusive());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
