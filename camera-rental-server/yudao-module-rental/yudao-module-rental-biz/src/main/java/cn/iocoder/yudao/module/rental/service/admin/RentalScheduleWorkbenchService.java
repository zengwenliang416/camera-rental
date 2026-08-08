package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchDeviceLaneRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchExceptionRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchMetricsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchPendingAllocationRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchSegmentRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchWindowRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleWorkbenchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RentalScheduleWorkbenchService {

    static final String SCHEDULE_EFFECTIVE = "EFFECTIVE";
    static final String ORDER_PENDING_ALLOCATION = "PENDING_ALLOCATION";
    static final String STATUS_ASSIGNED = "ASSIGNED";
    static final String STATUS_DISPATCHED = "DISPATCHED";
    static final String DEVICE_RENTED = "RENTED";
    static final String DEVICE_MAINTENANCE = "MAINTENANCE";
    static final String LOGISTICS_NONE = "NONE";
    static final String LOGISTICS_RETURNED_PENDING_INSPECTION = "RETURNED_PENDING_INSPECTION";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> ACTIVE_ASSIGNMENT_STATUSES = Set.of(STATUS_ASSIGNED, STATUS_DISPATCHED);
    private static final Set<String> TRANSIT_STATUSES =
            Set.of("PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "RETURNING");

    private final RentalScheduleWorkbenchMapper workbenchMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeliveryDeviceRelMapper deliveryRelationMapper;
    private final RentalDeliveryMapper deliveryMapper;
    private final RentalManualReviewMapper manualReviewMapper;
    private final RentalOrderItemMapper orderItemMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final Clock clock;

    @Autowired
    public RentalScheduleWorkbenchService(RentalScheduleWorkbenchMapper workbenchMapper,
                                          RentalDeviceAssignmentMapper assignmentMapper,
                                          RentalDeliveryDeviceRelMapper deliveryRelationMapper,
                                          RentalDeliveryMapper deliveryMapper,
                                          RentalManualReviewMapper manualReviewMapper,
                                          RentalOrderItemMapper orderItemMapper,
                                          RentalOrderMapper orderMapper,
                                          RentalScheduleMapper scheduleMapper) {
        this(workbenchMapper, assignmentMapper, deliveryRelationMapper, deliveryMapper, manualReviewMapper,
                orderItemMapper, orderMapper, scheduleMapper, Clock.system(BUSINESS_ZONE));
    }

    RentalScheduleWorkbenchService(RentalScheduleWorkbenchMapper workbenchMapper,
                                   RentalDeviceAssignmentMapper assignmentMapper,
                                   RentalDeliveryDeviceRelMapper deliveryRelationMapper,
                                   RentalDeliveryMapper deliveryMapper,
                                   RentalManualReviewMapper manualReviewMapper,
                                   RentalOrderItemMapper orderItemMapper,
                                   RentalOrderMapper orderMapper,
                                   RentalScheduleMapper scheduleMapper,
                                   Clock clock) {
        this.workbenchMapper = workbenchMapper;
        this.assignmentMapper = assignmentMapper;
        this.deliveryRelationMapper = deliveryRelationMapper;
        this.deliveryMapper = deliveryMapper;
        this.manualReviewMapper = manualReviewMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderMapper = orderMapper;
        this.scheduleMapper = scheduleMapper;
        this.clock = clock;
    }

    public RentalScheduleWorkbenchRespVO getWorkbench(RentalScheduleWorkbenchReqVO reqVO) {
        Window window = resolveWindow(reqVO);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String keyword = trimToNull(reqVO.getKeyword());
        String equipmentModelCode = trimToNull(reqVO.getEquipmentModelCode());
        String deviceStatus = trimToNull(reqVO.getDeviceStatus());
        String logisticsStatus = trimToNull(reqVO.getLogisticsStatus());

        IPage<RentalDeviceDO> page = workbenchMapper.selectDevicePage(
                MyBatisUtils.buildPage(reqVO), tenantId, keyword, equipmentModelCode, deviceStatus, logisticsStatus);
        List<RentalDeviceDO> devices = page == null || page.getRecords() == null
                ? List.of() : page.getRecords();
        List<Long> deviceIds = distinctIds(devices, RentalDeviceDO::getId);

        List<RentalScheduleDO> schedules = deviceIds.isEmpty() ? List.of() : scheduleMapper.selectList(
                new LambdaQueryWrapper<RentalScheduleDO>()
                        .eq(RentalScheduleDO::getTenantId, tenantId)
                        .in(RentalScheduleDO::getDeviceId, deviceIds)
                        .eq(RentalScheduleDO::getStatus, SCHEDULE_EFFECTIVE)
                        .lt(RentalScheduleDO::getOccupyStartDate, window.toDateExclusive())
                        .gt(RentalScheduleDO::getOccupyEndDateExclusive, window.fromDate())
                        .orderByAsc(RentalScheduleDO::getOccupyStartDate)
                        .orderByAsc(RentalScheduleDO::getId));
        schedules = nullSafe(schedules);

        Set<Long> scheduleOrderIds = schedules.stream()
                .map(RentalScheduleDO::getRentalOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<RentalOrderDO> pendingOrders = nullSafe(orderMapper.selectList(
                new LambdaQueryWrapper<RentalOrderDO>()
                        .eq(RentalOrderDO::getTenantId, tenantId)
                        .and(wrapper -> {
                            wrapper.eq(RentalOrderDO::getStatus, ORDER_PENDING_ALLOCATION);
                            if (!scheduleOrderIds.isEmpty()) {
                                wrapper.or().in(RentalOrderDO::getId, scheduleOrderIds);
                            }
                        })
                        .orderByAsc(RentalOrderDO::getId)));
        List<Long> orderIds = pendingOrders.stream().map(RentalOrderDO::getId)
                .filter(Objects::nonNull).distinct().toList();
        List<RentalOrderItemDO> orderItems = orderIds.isEmpty() ? List.of() : nullSafe(orderItemMapper.selectList(
                new LambdaQueryWrapper<RentalOrderItemDO>()
                        .eq(RentalOrderItemDO::getTenantId, tenantId)
                        .in(RentalOrderItemDO::getRentalOrderId, orderIds)
                        .orderByAsc(RentalOrderItemDO::getRentalOrderId)
                        .orderByAsc(RentalOrderItemDO::getId)));

        Set<Long> assignmentOrderIds = new LinkedHashSet<>(orderIds);
        Set<Long> assignmentDeviceIds = new LinkedHashSet<>(deviceIds);
        List<RentalDeviceAssignmentDO> assignments = assignmentOrderIds.isEmpty() && assignmentDeviceIds.isEmpty()
                ? List.of() : nullSafe(assignmentMapper.selectList(
                new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                        .eq(RentalDeviceAssignmentDO::getTenantId, tenantId)
                        .in(RentalDeviceAssignmentDO::getStatus, ACTIVE_ASSIGNMENT_STATUSES)
                        .and(wrapper -> {
                            if (!assignmentDeviceIds.isEmpty()) {
                                wrapper.in(RentalDeviceAssignmentDO::getDeviceId, assignmentDeviceIds);
                            }
                            if (!assignmentOrderIds.isEmpty()) {
                                if (!assignmentDeviceIds.isEmpty()) {
                                    wrapper.or();
                                }
                                wrapper.in(RentalDeviceAssignmentDO::getRentalOrderId, assignmentOrderIds);
                            }
                        })
                        .orderByAsc(RentalDeviceAssignmentDO::getId)));

        List<RentalDeliveryDeviceRelDO> deliveryRelations = deviceIds.isEmpty()
                ? List.of() : nullSafe(deliveryRelationMapper.selectList(
                new LambdaQueryWrapper<RentalDeliveryDeviceRelDO>()
                        .eq(RentalDeliveryDeviceRelDO::getTenantId, tenantId)
                        .in(RentalDeliveryDeviceRelDO::getDeviceId, deviceIds)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getDeviceId)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getId)));
        Set<Long> deliveryIds = deliveryRelations.stream().map(RentalDeliveryDeviceRelDO::getDeliveryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<RentalDeliveryDO> deliveries = deliveryIds.isEmpty() ? List.of() : nullSafe(deliveryMapper.selectList(
                new LambdaQueryWrapper<RentalDeliveryDO>()
                        .eq(RentalDeliveryDO::getTenantId, tenantId)
                        .in(RentalDeliveryDO::getId, deliveryIds)
                        .eq(RentalDeliveryDO::getLifecycleStatus, "ACTIVE")));
        List<RentalManualReviewDO> reviews = nullSafe(manualReviewMapper.selectList(
                new LambdaQueryWrapper<RentalManualReviewDO>()
                        .eq(RentalManualReviewDO::getTenantId, tenantId)
                        .eq(RentalManualReviewDO::getStatus, "OPEN")
                        .orderByAsc(RentalManualReviewDO::getId)));

        Map<Long, RentalOrderItemDO> orderItemsById = indexById(orderItems, RentalOrderItemDO::getId);
        Map<Long, RentalOrderDO> ordersById = indexById(pendingOrders, RentalOrderDO::getId);
        Map<Long, RentalDeviceAssignmentDO> assignmentsByScheduleId = assignments.stream()
                .filter(assignment -> assignment.getScheduleId() != null)
                .collect(Collectors.toMap(RentalDeviceAssignmentDO::getScheduleId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<RentalDeviceAssignmentDO>> assignmentsByItemId = assignments.stream()
                .filter(assignment -> assignment.getRentalOrderItemId() != null)
                .collect(Collectors.groupingBy(RentalDeviceAssignmentDO::getRentalOrderItemId,
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<RentalDeliveryDO>> deliveriesByDeviceId = deliveriesByDeviceId(deliveryRelations, deliveries);
        Map<Long, List<RentalScheduleDO>> schedulesByDeviceId = schedules.stream()
                .filter(schedule -> schedule.getDeviceId() != null)
                .collect(Collectors.groupingBy(RentalScheduleDO::getDeviceId, LinkedHashMap::new,
                        Collectors.toList()));

        List<RentalScheduleWorkbenchDeviceLaneRespVO> lanes = devices.stream()
                .map(device -> toLane(device, schedulesByDeviceId.getOrDefault(device.getId(), List.of()),
                        orderItemsById, ordersById, assignmentsByScheduleId,
                        deliveriesByDeviceId.getOrDefault(device.getId(), List.of()), window))
                .toList();
        List<RentalScheduleWorkbenchPendingAllocationRespVO> pendingAllocations =
                toPendingAllocations(pendingOrders, orderItems, assignmentsByItemId);
        List<RentalScheduleWorkbenchExceptionRespVO> exceptions =
                toExceptions(devices, lanes, deliveriesByDeviceId, reviews);

        RentalScheduleWorkbenchMetricsRespVO metrics = workbenchMapper.selectDeviceMetrics(
                tenantId, window.fromDate(), window.toDateExclusive(), keyword, equipmentModelCode,
                deviceStatus, logisticsStatus);
        metrics = metrics == null ? fallbackMetrics(lanes, page == null ? 0L : page.getTotal(), window) : copy(metrics);
        metrics.setPendingAllocationCount((long) pendingAllocations.size());
        metrics.setPendingAllocationItems((long) pendingAllocations.size());
        metrics.setPendingAllocationOrders(pendingAllocations.stream()
                .map(RentalScheduleWorkbenchPendingAllocationRespVO::getRentalOrderId)
                .filter(Objects::nonNull).distinct().count());
        metrics.setExceptionCount((long) exceptions.size());
        metrics.setTotalDeviceDays(safeLong(metrics.getTotalDevices()) * window.dayCount());
        metrics.setUtilizationRate(utilization(metrics.getOccupiedDeviceDays(), metrics.getTotalDeviceDays()));

        RentalScheduleWorkbenchRespVO response = new RentalScheduleWorkbenchRespVO();
        response.setWindow(toWindowResp(window));
        response.setMetrics(metrics);
        response.setDevicePage(new PageResult<>(lanes, page == null ? 0L : page.getTotal()));
        response.setPendingAllocations(pendingAllocations);
        response.setExceptions(exceptions);
        return response;
    }

    private RentalScheduleWorkbenchDeviceLaneRespVO toLane(
            RentalDeviceDO device, List<RentalScheduleDO> schedules,
            Map<Long, RentalOrderItemDO> orderItemsById, Map<Long, RentalOrderDO> ordersById,
            Map<Long, RentalDeviceAssignmentDO> assignmentsByScheduleId,
            List<RentalDeliveryDO> deliveries, Window window) {
        String logisticsStatus = resolveLogisticsStatus(device, deliveries);
        RentalScheduleWorkbenchDeviceLaneRespVO lane = new RentalScheduleWorkbenchDeviceLaneRespVO();
        lane.setDeviceId(device.getId());
        lane.setDeviceNo(device.getDeviceNo());
        lane.setLegacyDeviceNo(device.getLegacyDeviceNo());
        lane.setSerialNumber(device.getSerialNumber());
        lane.setEquipmentModelCode(device.getEquipmentModelCode());
        lane.setWarehouseCode(device.getWarehouseCode());
        lane.setEnabled(device.getEnabled());
        lane.setDeviceStatus(device.getStatus());
        lane.setLogisticsStatus(logisticsStatus);
        lane.setOccupied(isOccupied(device, schedules));
        lane.setExpectedReleaseDate(expectedReleaseDate(device, schedules));
        lane.setSegments(schedules.stream()
                .filter(schedule -> validRange(schedule.getOccupyStartDate(), schedule.getOccupyEndDateExclusive()))
                .filter(schedule -> schedule.getOccupyStartDate().isBefore(window.toDateExclusive())
                        && schedule.getOccupyEndDateExclusive().isAfter(window.fromDate()))
                .map(schedule -> toSegment(schedule, orderItemsById.get(schedule.getRentalOrderItemId()),
                        ordersById.get(schedule.getRentalOrderId()), assignmentsByScheduleId.get(schedule.getId()),
                        logisticsStatus, window))
                .toList());
        return lane;
    }

    private RentalScheduleWorkbenchSegmentRespVO toSegment(
            RentalScheduleDO schedule, RentalOrderItemDO item, RentalOrderDO order,
            RentalDeviceAssignmentDO assignment, String logisticsStatus, Window window) {
        LocalDate displayStart = max(schedule.getOccupyStartDate(), window.fromDate());
        LocalDate displayEnd = min(schedule.getOccupyEndDateExclusive(), window.toDateExclusive());
        RentalScheduleWorkbenchSegmentRespVO segment = new RentalScheduleWorkbenchSegmentRespVO();
        segment.setScheduleId(schedule.getId());
        segment.setRentalOrderId(schedule.getRentalOrderId());
        segment.setRentalOrderItemId(schedule.getRentalOrderItemId());
        segment.setAssignmentId(assignment == null ? null : assignment.getId());
        segment.setOrderNo(order == null ? null : order.getOrderNo());
        segment.setSegmentType("OCCUPIED");
        segment.setScheduleType(schedule.getScheduleType());
        segment.setStatus(schedule.getStatus());
        segment.setLogisticsStatus(logisticsStatus);
        if (item != null) {
            segment.setBillableStartDate(item.getBillableStartDate());
            segment.setBillableEndDate(item.getBillableEndDate());
        }
        segment.setOccupyStartDate(schedule.getOccupyStartDate());
        segment.setOccupyEndDateExclusive(schedule.getOccupyEndDateExclusive());
        segment.setDisplayStartDate(displayStart);
        segment.setDisplayEndDateExclusive(displayEnd);
        segment.setLeftContinuation(schedule.getOccupyStartDate().isBefore(window.fromDate()));
        segment.setRightContinuation(schedule.getOccupyEndDateExclusive().isAfter(window.toDateExclusive()));
        return segment;
    }

    private List<RentalScheduleWorkbenchPendingAllocationRespVO> toPendingAllocations(
            List<RentalOrderDO> orders, List<RentalOrderItemDO> items,
            Map<Long, List<RentalDeviceAssignmentDO>> assignmentsByItemId) {
        Map<Long, RentalOrderDO> ordersById = indexById(orders, RentalOrderDO::getId);
        List<RentalScheduleWorkbenchPendingAllocationRespVO> result = new ArrayList<>();
        for (RentalOrderItemDO item : items) {
            RentalOrderDO order = ordersById.get(item.getRentalOrderId());
            if (order == null || !ORDER_PENDING_ALLOCATION.equals(order.getStatus())) {
                continue;
            }
            int required = item.getQuantity() == null ? 0 : item.getQuantity();
            int assigned = assignmentsByItemId.getOrDefault(item.getId(), List.of()).size();
            int remaining = Math.max(0, required - assigned);
            if (remaining == 0) {
                continue;
            }
            RentalScheduleWorkbenchPendingAllocationRespVO vo =
                    new RentalScheduleWorkbenchPendingAllocationRespVO();
            vo.setRentalOrderId(order.getId());
            vo.setRentalOrderItemId(item.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setOrderStatus(order.getStatus());
            vo.setEquipmentModelCode(item.getEquipmentModelCode());
            vo.setRequiredQuantity(required);
            vo.setAssignedQuantity(assigned);
            vo.setRemainingQuantity(remaining);
            vo.setBillableStartDate(item.getBillableStartDate());
            vo.setBillableEndDate(item.getBillableEndDate());
            vo.setOccupyStartDate(item.getOccupyStartDate());
            vo.setOccupyEndDateExclusive(item.getOccupyEndDateExclusive());
            result.add(vo);
        }
        return result;
    }

    private List<RentalScheduleWorkbenchExceptionRespVO> toExceptions(
            List<RentalDeviceDO> devices, List<RentalScheduleWorkbenchDeviceLaneRespVO> lanes,
            Map<Long, List<RentalDeliveryDO>> deliveriesByDeviceId, List<RentalManualReviewDO> reviews) {
        Map<Long, RentalScheduleWorkbenchDeviceLaneRespVO> lanesById = lanes.stream()
                .collect(Collectors.toMap(RentalScheduleWorkbenchDeviceLaneRespVO::getDeviceId,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<RentalScheduleWorkbenchExceptionRespVO> result = new ArrayList<>();
        Set<String> dedup = new LinkedHashSet<>();
        for (RentalDeviceDO device : devices) {
            RentalScheduleWorkbenchDeviceLaneRespVO lane = lanesById.get(device.getId());
            for (RentalDeliveryDO delivery : deliveriesByDeviceId.getOrDefault(device.getId(), List.of())) {
                if ("RETURNED_PENDING_INSPECTION".equals(lane.getLogisticsStatus())
                        && "RETURN".equals(delivery.getDirection())
                        && "RETURNED".equals(delivery.getTrackingStatus())) {
                    addException(result, dedup, exception("RETURN_INSPECTION_PENDING", "HIGH",
                            "设备已回仓但尚未完成检测", "完成回仓检测后再释放设备",
                            device, delivery.getRentalOrderId(), null, null, null));
                }
                if ("EXCEPTION".equals(delivery.getTrackingStatus())) {
                    addException(result, dedup, exception("LOGISTICS_EXCEPTION", "HIGH",
                            "物流快照标记为异常", "查看本地物流快照并处理物流异常",
                            device, delivery.getRentalOrderId(), null, "DELIVERY", delivery.getId()));
                }
                if (isStale(delivery)) {
                    addException(result, dedup, exception("TRACKING_STALE", "MEDIUM",
                            "物流快照超过 24 小时未更新", "按需刷新物流详情",
                            device, delivery.getRentalOrderId(), null, "DELIVERY", delivery.getId()));
                }
            }
        }
        for (RentalManualReviewDO review : reviews) {
            addException(result, dedup, exception("MANUAL_REVIEW_OPEN", "MEDIUM",
                    review.getReasonMessage(), "处理待复核记录",
                    null, null, null, "MANUAL_REVIEW",
                    review.getId()));
        }
        return result;
    }

    private RentalScheduleWorkbenchExceptionRespVO exception(String code, String severity, String message,
                                                              String nextAction, RentalDeviceDO device,
                                                              Long orderId, Long itemId, String sourceType,
                                                              Long sourceId) {
        RentalScheduleWorkbenchExceptionRespVO vo = new RentalScheduleWorkbenchExceptionRespVO();
        vo.setCode(code);
        vo.setSeverity(severity);
        vo.setMessage(message);
        vo.setNextAction(nextAction);
        if (device != null) {
            vo.setDeviceId(device.getId());
            vo.setDeviceNo(device.getDeviceNo());
        }
        vo.setRentalOrderId(orderId);
        vo.setRentalOrderItemId(itemId);
        vo.setSourceType(sourceType);
        vo.setSourceId(sourceId == null ? null : sourceId.toString());
        return vo;
    }

    private void addException(List<RentalScheduleWorkbenchExceptionRespVO> result, Set<String> dedup,
                              RentalScheduleWorkbenchExceptionRespVO exception) {
        String key = exception.getCode() + ":" + exception.getDeviceId() + ":" + exception.getSourceId();
        if (dedup.add(key)) {
            result.add(exception);
        }
    }

    private RentalScheduleWorkbenchMetricsRespVO fallbackMetrics(
            List<RentalScheduleWorkbenchDeviceLaneRespVO> lanes, long totalDevices, Window window) {
        RentalScheduleWorkbenchMetricsRespVO metrics = new RentalScheduleWorkbenchMetricsRespVO();
        metrics.setTotalDevices(totalDevices);
        metrics.setAvailableDevices(lanes.stream()
                .filter(lane -> Boolean.TRUE.equals(lane.getEnabled()))
                .filter(lane -> "AVAILABLE".equals(lane.getDeviceStatus()))
                .filter(lane -> !Boolean.TRUE.equals(lane.getOccupied()))
                .count());
        metrics.setOccupiedDevices(lanes.stream().filter(lane -> Boolean.TRUE.equals(lane.getOccupied())).count());
        metrics.setInTransitDevices(lanes.stream().filter(lane -> TRANSIT_STATUSES.contains(lane.getLogisticsStatus())).count());
        metrics.setOccupiedDeviceDays(lanes.stream()
                .flatMap(lane -> lane.getSegments().stream())
                .mapToLong(segment -> ChronoUnit.DAYS.between(
                        segment.getDisplayStartDate(), segment.getDisplayEndDateExclusive()))
                .sum());
        return metrics;
    }

    private RentalScheduleWorkbenchMetricsRespVO copy(RentalScheduleWorkbenchMetricsRespVO source) {
        RentalScheduleWorkbenchMetricsRespVO target = new RentalScheduleWorkbenchMetricsRespVO();
        target.setTotalDevices(safeLong(source.getTotalDevices()));
        target.setAvailableDevices(safeLong(source.getAvailableDevices()));
        target.setOccupiedDevices(safeLong(source.getOccupiedDevices()));
        target.setInTransitDevices(safeLong(source.getInTransitDevices()));
        target.setOccupiedDeviceDays(safeLong(source.getOccupiedDeviceDays()));
        return target;
    }

    private RentalScheduleWorkbenchWindowRespVO toWindowResp(Window window) {
        RentalScheduleWorkbenchWindowRespVO vo = new RentalScheduleWorkbenchWindowRespVO();
        vo.setFromDate(window.fromDate());
        vo.setToDateExclusive(window.toDateExclusive());
        vo.setViewMode(window.viewMode());
        vo.setDayCount(window.dayCount());
        vo.setTimezone(BUSINESS_ZONE.getId());
        return vo;
    }

    private Window resolveWindow(RentalScheduleWorkbenchReqVO reqVO) {
        LocalDate fromDate = reqVO.getFromDate() == null
                ? LocalDateTime.now(clock).toLocalDate() : reqVO.getFromDate();
        String requestedMode = normalizeViewMode(reqVO.getViewMode());
        LocalDate toDateExclusive = reqVO.getToDateExclusive() == null
                ? fromDate.plusDays(viewDays(requestedMode)) : reqVO.getToDateExclusive();
        if (!fromDate.isBefore(toDateExclusive)) {
            throw new IllegalArgumentException("fromDate must be before toDateExclusive");
        }
        long dayCount = ChronoUnit.DAYS.between(fromDate, toDateExclusive);
        if (dayCount > 90) {
            throw new IllegalArgumentException("Workbench window cannot exceed 90 days");
        }
        String viewMode = dayCount == 14 || dayCount == 30 || dayCount == 90
                ? dayCount + "D" : requestedMode;
        return new Window(fromDate, toDateExclusive, viewMode, Math.toIntExact(dayCount));
    }

    private String normalizeViewMode(String value) {
        if (value == null || value.isBlank()) {
            return "30D";
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("14")) {
            return "14D";
        }
        if (normalized.contains("90")) {
            return "90D";
        }
        if (normalized.contains("30")) {
            return "30D";
        }
        if (normalized.contains("PRECISE")) {
            return "14D";
        }
        if (normalized.contains("LONG")) {
            return "90D";
        }
        return "30D";
    }

    private int viewDays(String viewMode) {
        return Integer.parseInt(viewMode.substring(0, viewMode.length() - 1));
    }

    private String resolveLogisticsStatus(RentalDeviceDO device, List<RentalDeliveryDO> deliveries) {
        if (deliveries.stream().anyMatch(delivery -> "EXCEPTION".equals(delivery.getTrackingStatus()))) {
            return "EXCEPTION";
        }
        if (DEVICE_RENTED.equals(device.getStatus()) && deliveries.stream()
                .anyMatch(delivery -> "RETURN".equals(delivery.getDirection())
                        && "RETURNED".equals(delivery.getTrackingStatus()))) {
            return LOGISTICS_RETURNED_PENDING_INSPECTION;
        }
        return deliveries.stream()
                .filter(delivery -> delivery.getTrackingStatus() != null)
                .sorted(Comparator.comparing(RentalDeliveryDO::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(RentalDeliveryDO::getTrackingStatus)
                .findFirst()
                .orElse(LOGISTICS_NONE);
    }

    private boolean isOccupied(RentalDeviceDO device, List<RentalScheduleDO> schedules) {
        return DEVICE_RENTED.equals(device.getStatus())
                || DEVICE_MAINTENANCE.equals(device.getStatus())
                || !schedules.isEmpty();
    }

    private LocalDate expectedReleaseDate(RentalDeviceDO device, List<RentalScheduleDO> schedules) {
        LocalDate today = LocalDateTime.now(clock).toLocalDate();
        return schedules.stream()
                .filter(schedule -> schedule.getOccupyStartDate() != null
                        && schedule.getOccupyEndDateExclusive() != null
                        && !schedule.getOccupyStartDate().isAfter(today)
                        && schedule.getOccupyEndDateExclusive().isAfter(today))
                .map(RentalScheduleDO::getOccupyEndDateExclusive)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private Map<Long, List<RentalDeliveryDO>> deliveriesByDeviceId(
            List<RentalDeliveryDeviceRelDO> relations, List<RentalDeliveryDO> deliveries) {
        Map<Long, RentalDeliveryDO> deliveriesById = indexById(deliveries, RentalDeliveryDO::getId);
        Map<Long, List<RentalDeliveryDO>> result = new LinkedHashMap<>();
        for (RentalDeliveryDeviceRelDO relation : relations) {
            RentalDeliveryDO delivery = deliveriesById.get(relation.getDeliveryId());
            if (delivery != null && relation.getDeviceId() != null) {
                result.computeIfAbsent(relation.getDeviceId(), ignored -> new ArrayList<>()).add(delivery);
            }
        }
        return result;
    }

    private boolean isStale(RentalDeliveryDO delivery) {
        return delivery.getLastSyncedAt() != null
                && delivery.getLastSyncedAt().isBefore(LocalDateTime.now(clock).minusHours(24))
                && !"DELIVERED".equals(delivery.getTrackingStatus())
                && !"RETURNED".equals(delivery.getTrackingStatus());
    }

    private BigDecimal utilization(Long occupiedDays, Long totalDays) {
        if (safeLong(totalDays) == 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(safeLong(occupiedDays))
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean validRange(LocalDate start, LocalDate endExclusive) {
        return start != null && endExclusive != null && start.isBefore(endExclusive);
    }

    private LocalDate max(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <T> Map<Long, T> indexById(Collection<T> values, Function<T, Long> idExtractor) {
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> idExtractor.apply(value) != null)
                .collect(Collectors.toMap(idExtractor, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
    }

    private <T> List<Long> distinctIds(Collection<T> values, Function<T, Long> idExtractor) {
        return values.stream().map(idExtractor).filter(Objects::nonNull).distinct().toList();
    }

    private record Window(LocalDate fromDate, LocalDate toDateExclusive, String viewMode, int dayCount) {
    }
}
