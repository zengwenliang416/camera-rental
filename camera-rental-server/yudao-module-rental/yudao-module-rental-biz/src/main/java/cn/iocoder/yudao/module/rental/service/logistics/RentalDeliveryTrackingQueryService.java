package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingDeviceRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingOrderSummaryRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingPackageSummaryRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingRiskRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingTraceRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryTraceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryTraceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RentalDeliveryTrackingQueryService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long STALE_HOURS = 24;
    private static final Set<String> TERMINAL_STATUSES = Set.of("DELIVERED", "RETURNED");

    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryDeviceRelMapper relationMapper;
    private final RentalDeliveryTraceMapper traceMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalScheduleMapper scheduleMapper;
    private final WaybillPrivacy waybillPrivacy;
    private final SensitiveValueRedactor redactor;
    private final RentalLogisticsRiskService riskService;
    private final Clock clock;

    @Autowired
    public RentalDeliveryTrackingQueryService(RentalDeliveryMapper deliveryMapper,
                                              RentalDeliveryDeviceRelMapper relationMapper,
                                              RentalDeliveryTraceMapper traceMapper,
                                              RentalOrderMapper orderMapper,
                                              RentalDeviceMapper deviceMapper,
                                              RentalScheduleMapper scheduleMapper,
                                              WaybillPrivacy waybillPrivacy,
                                              SensitiveValueRedactor redactor,
                                              RentalLogisticsRiskService riskService) {
        this(deliveryMapper, relationMapper, traceMapper, orderMapper, deviceMapper, scheduleMapper,
                waybillPrivacy, redactor, riskService, Clock.system(BUSINESS_ZONE));
    }

    RentalDeliveryTrackingQueryService(RentalDeliveryMapper deliveryMapper,
                                       RentalDeliveryDeviceRelMapper relationMapper,
                                       RentalDeliveryTraceMapper traceMapper,
                                       RentalOrderMapper orderMapper,
                                       RentalDeviceMapper deviceMapper,
                                       RentalScheduleMapper scheduleMapper,
                                       WaybillPrivacy waybillPrivacy,
                                       SensitiveValueRedactor redactor,
                                       RentalLogisticsRiskService riskService,
                                       Clock clock) {
        this.deliveryMapper = deliveryMapper;
        this.relationMapper = relationMapper;
        this.traceMapper = traceMapper;
        this.orderMapper = orderMapper;
        this.deviceMapper = deviceMapper;
        this.scheduleMapper = scheduleMapper;
        this.waybillPrivacy = waybillPrivacy;
        this.redactor = redactor;
        this.riskService = riskService;
        this.clock = clock;
    }

    public Map<Long, RentalDeliveryTrackingOrderSummaryRespVO> getSummaries(List<Long> orderIds) {
        List<Long> requestedOrderIds = normalizeIds(orderIds);
        if (requestedOrderIds.isEmpty()) {
            return Map.of();
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<RentalDeliveryDO> deliveries = loadDeliveries(tenantId, requestedOrderIds);
        List<Long> rentalOrderIds = deliveries.stream()
                .map(RentalDeliveryDO::getRentalOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<RentalOrderDO> orders = rentalOrderIds.isEmpty()
                ? List.of()
                : nullSafe(orderMapper.selectByIds(rentalOrderIds));
        ReadModelContext context = loadContext(tenantId, deliveries);
        Map<Long, RentalOrderDO> ordersById = indexById(orders, RentalOrderDO::getId);
        LocalDateTime now = now();

        Map<Long, RentalDeliveryTrackingOrderSummaryRespVO> result = new LinkedHashMap<>();
        for (Long orderId : requestedOrderIds) {
            List<RentalDeliveryDO> orderDeliveries = deliveries.stream()
                    .filter(delivery -> Objects.equals(orderId, delivery.getRentalOrderId())
                            || Objects.equals(orderId, delivery.getChannelOrderId()))
                    .toList();
            Long rentalOrderId = orderDeliveries.stream()
                    .map(RentalDeliveryDO::getRentalOrderId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            List<RentalLogisticsRisk> risks = riskService.evaluate(
                    ordersById.get(rentalOrderId), orderDeliveries, context.deviceIdsByDeliveryId(),
                    context.schedules(), now);
            result.put(orderId, toOrderSummary(orderId, orderDeliveries, risks,
                    context.deviceIdsByDeliveryId(), now));
        }
        return result;
    }

    public RentalDeliveryTrackingDetailRespVO getDetail(Long deliveryId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndId(tenantId, deliveryId);
        if (delivery == null) {
            return null;
        }

        RentalOrderDO order = delivery.getRentalOrderId() == null
                ? null : orderMapper.selectById(delivery.getRentalOrderId());
        Long referenceId = referenceId(delivery);
        List<RentalDeliveryDO> orderDeliveries = loadDeliveries(tenantId, List.of(referenceId));
        ReadModelContext context = loadContext(tenantId, orderDeliveries);
        List<RentalLogisticsRisk> risks = riskService.evaluate(
                order, orderDeliveries, context.deviceIdsByDeliveryId(), context.schedules(), now());
        List<RentalDeliveryTraceDO> traces = delivery.getTrackingVersion() == null
                ? List.of()
                : nullSafe(traceMapper.selectSnapshot(tenantId, deliveryId, delivery.getTrackingVersion()));
        return toDetail(delivery, traces, risks, context);
    }

    private List<RentalDeliveryDO> loadDeliveries(Long tenantId, List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return nullSafe(deliveryMapper.selectList(new LambdaQueryWrapper<RentalDeliveryDO>()
                .eq(RentalDeliveryDO::getTenantId, tenantId)
                .and(wrapper -> wrapper.in(RentalDeliveryDO::getRentalOrderId, orderIds)
                        .or()
                        .in(RentalDeliveryDO::getChannelOrderId, orderIds))
                .orderByAsc(RentalDeliveryDO::getId)));
    }

    private ReadModelContext loadContext(Long tenantId, List<RentalDeliveryDO> deliveries) {
        List<Long> deliveryIds = deliveries.stream()
                .map(RentalDeliveryDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (deliveryIds.isEmpty()) {
            return new ReadModelContext(Map.of(), Map.of(), List.of());
        }

        List<RentalDeliveryDeviceRelDO> relations = nullSafe(relationMapper.selectList(
                new LambdaQueryWrapper<RentalDeliveryDeviceRelDO>()
                        .eq(RentalDeliveryDeviceRelDO::getTenantId, tenantId)
                        .in(RentalDeliveryDeviceRelDO::getDeliveryId, deliveryIds)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getDeliveryId)
                        .orderByAsc(RentalDeliveryDeviceRelDO::getId)));
        Map<Long, List<Long>> deviceIdsByDeliveryId = relations.stream()
                .filter(relation -> relation.getDeliveryId() != null && relation.getDeviceId() != null)
                .collect(Collectors.groupingBy(RentalDeliveryDeviceRelDO::getDeliveryId,
                        LinkedHashMap::new,
                        Collectors.mapping(RentalDeliveryDeviceRelDO::getDeviceId, Collectors.toList())));
        List<Long> deviceIds = relations.stream()
                .map(RentalDeliveryDeviceRelDO::getDeviceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (deviceIds.isEmpty()) {
            return new ReadModelContext(deviceIdsByDeliveryId, Map.of(), List.of());
        }

        Map<Long, RentalDeviceDO> devicesById = indexById(
                nullSafe(deviceMapper.selectByIds(deviceIds)), RentalDeviceDO::getId);
        List<RentalScheduleDO> schedules = nullSafe(scheduleMapper.selectList(
                new LambdaQueryWrapper<RentalScheduleDO>()
                        .eq(RentalScheduleDO::getTenantId, tenantId)
                        .in(RentalScheduleDO::getDeviceId, deviceIds)
                        .eq(RentalScheduleDO::getStatus, "EFFECTIVE")
                        .orderByAsc(RentalScheduleDO::getOccupyStartDate)
                        .orderByAsc(RentalScheduleDO::getId)));
        return new ReadModelContext(deviceIdsByDeliveryId, devicesById, schedules);
    }

    private RentalDeliveryTrackingOrderSummaryRespVO toOrderSummary(
            Long orderId,
            List<RentalDeliveryDO> deliveries,
            List<RentalLogisticsRisk> risks,
            Map<Long, List<Long>> deviceIdsByDeliveryId,
            LocalDateTime now) {
        RentalDeliveryTrackingOrderSummaryRespVO result =
                new RentalDeliveryTrackingOrderSummaryRespVO();
        result.setOrderId(orderId);
        result.setRentalOrderId(orderId);
        result.setPackageCount(deliveries.size());
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        List<RentalDeliveryTrackingPackageSummaryRespVO> packages = new ArrayList<>();
        for (RentalDeliveryDO delivery : deliveries) {
            String trackingStatus = safeStatus(delivery.getTrackingStatus());
            statusCounts.merge(trackingStatus, 1, Integer::sum);
            RentalDeliveryTrackingPackageSummaryRespVO packageSummary =
                    toPackageSummary(delivery, now);
            packageSummary.setRisk(firstRiskForDelivery(
                    risks, deviceIdsByDeliveryId.getOrDefault(delivery.getId(), List.of())));
            packages.add(packageSummary);
        }
        result.setStatusCounts(statusCounts);
        result.setPackages(packages);
        result.setRisks(risks.stream().map(this::toRisk).toList());
        return result;
    }

    private RentalDeliveryTrackingPackageSummaryRespVO toPackageSummary(
            RentalDeliveryDO delivery, LocalDateTime now) {
        RentalDeliveryTrackingPackageSummaryRespVO result =
                new RentalDeliveryTrackingPackageSummaryRespVO();
        result.setDeliveryId(delivery.getId());
        result.setRentalOrderId(referenceId(delivery));
        result.setDirection(delivery.getDirection());
        result.setPackageSeq(delivery.getPackageSeq());
        result.setCarrierName(safeCarrierName(delivery));
        result.setMaskedWaybillNo(maskWaybill(delivery.getWaybillNo()));
        result.setTrackingStatus(safeStatus(delivery.getTrackingStatus()));
        result.setMappingStatus(delivery.getMappingStatus());
        result.setSubscribeStatus(delivery.getSubscribeStatus());
        result.setQueryStatus(delivery.getQueryStatus());
        result.setLatestTraceText(redactor.redact(delivery.getLatestTraceText()));
        result.setLatestEventTime(delivery.getLatestEventTime());
        result.setLastSyncedAt(delivery.getLastSyncedAt());
        result.setEstimatedDeliveryAt(delivery.getEstimatedDeliveryAt());
        result.setStale(isStale(delivery, now));
        return result;
    }

    private RentalDeliveryTrackingDetailRespVO toDetail(
            RentalDeliveryDO delivery,
            List<RentalDeliveryTraceDO> traces,
            List<RentalLogisticsRisk> risks,
            ReadModelContext context) {
        RentalDeliveryTrackingDetailRespVO result = new RentalDeliveryTrackingDetailRespVO();
        result.setDeliveryId(delivery.getId());
        result.setRentalOrderId(referenceId(delivery));
        result.setDirection(delivery.getDirection());
        result.setPackageSeq(delivery.getPackageSeq());
        result.setCarrierName(safeCarrierName(delivery));
        result.setMaskedWaybillNo(maskWaybill(delivery.getWaybillNo()));
        result.setTrackingStatus(safeStatus(delivery.getTrackingStatus()));
        result.setMappingStatus(delivery.getMappingStatus());
        result.setSubscribeStatus(delivery.getSubscribeStatus());
        result.setQueryStatus(delivery.getQueryStatus());
        result.setLatestTraceText(redactor.redact(delivery.getLatestTraceText()));
        result.setLatestLocation(redactor.redact(delivery.getLatestLocation()));
        result.setLatestEventTime(delivery.getLatestEventTime());
        result.setLastSyncedAt(delivery.getLastSyncedAt());
        result.setEstimatedDeliveryAt(delivery.getEstimatedDeliveryAt());
        result.setNextQueryAllowedAt(delivery.getNextQueryAllowedAt());
        result.setStale(isStale(delivery, now()));

        List<Long> deviceIds = context.deviceIdsByDeliveryId()
                .getOrDefault(delivery.getId(), List.of());
        result.setDevices(deviceIds.stream()
                .map(context.devicesById()::get)
                .filter(Objects::nonNull)
                .map(this::toDevice)
                .toList());
        result.setTraces(traces.stream()
                .sorted(Comparator.comparing(RentalDeliveryTraceDO::getBusinessTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RentalDeliveryTraceDO::getEventSeq,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toTrace)
                .toList());
        result.setRisks(risks.stream().map(this::toRisk).toList());
        return result;
    }

    private RentalDeliveryTrackingDeviceRespVO toDevice(RentalDeviceDO device) {
        RentalDeliveryTrackingDeviceRespVO result = new RentalDeliveryTrackingDeviceRespVO();
        result.setDeviceId(device.getId());
        result.setDeviceNo(redactor.redact(device.getDeviceNo()));
        result.setEquipmentModelCode(redactor.redact(device.getEquipmentModelCode()));
        return result;
    }

    private RentalDeliveryTrackingTraceRespVO toTrace(RentalDeliveryTraceDO trace) {
        RentalDeliveryTrackingTraceRespVO result = new RentalDeliveryTrackingTraceRespVO();
        result.setEventSeq(trace.getEventSeq());
        result.setBusinessTime(trace.getBusinessTime());
        result.setTrackingStatus(safeStatus(trace.getTrackingStatus()));
        result.setTraceText(redactor.redact(trace.getTraceText()));
        result.setLocation(redactor.redact(trace.getLocation()));
        return result;
    }

    private RentalDeliveryTrackingRiskRespVO firstRiskForDelivery(
            List<RentalLogisticsRisk> risks, List<Long> deviceIds) {
        Set<Long> packageDeviceIds = Set.copyOf(deviceIds);
        return risks.stream()
                .filter(risk -> risk.affectedDeviceIds().isEmpty()
                        || risk.affectedDeviceIds().stream().anyMatch(packageDeviceIds::contains))
                .findFirst()
                .map(this::toRisk)
                .orElse(null);
    }

    private Long referenceId(RentalDeliveryDO delivery) {
        return delivery.getRentalOrderId() != null
                ? delivery.getRentalOrderId() : delivery.getChannelOrderId();
    }

    private RentalDeliveryTrackingRiskRespVO toRisk(RentalLogisticsRisk risk) {
        RentalDeliveryTrackingRiskRespVO result = new RentalDeliveryTrackingRiskRespVO();
        result.setCode(risk.code());
        result.setSeverity(risk.severity().toLowerCase(Locale.ROOT));
        result.setSafeMessage(redactor.redact(risk.safeMessage()));
        result.setNextAction(redactor.redact(risk.nextAction()));
        result.setDeviceIds(risk.affectedDeviceIds());
        return result;
    }

    private String safeCarrierName(RentalDeliveryDO delivery) {
        String carrier = delivery.getSourceCarrierName() != null
                ? delivery.getSourceCarrierName()
                : delivery.getCanonicalCarrierCode();
        return redactor.redact(carrier);
    }

    private String maskWaybill(String waybillNo) {
        if (waybillNo == null) {
            return null;
        }
        try {
            return waybillPrivacy.mask(waybillNo);
        } catch (RentalLogisticsException ignored) {
            return null;
        }
    }

    private String safeStatus(String status) {
        return status == null ? "UNKNOWN" : status;
    }

    private boolean isStale(RentalDeliveryDO delivery, LocalDateTime now) {
        return !TERMINAL_STATUSES.contains(delivery.getTrackingStatus())
                && delivery.getLastSyncedAt() != null
                && delivery.getLastSyncedAt().isBefore(now.minusHours(STALE_HOURS));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        LinkedHashSet<Long> normalized = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(normalized);
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <T> Map<Long, T> indexById(List<T> values, Function<T, Long> idExtractor) {
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> idExtractor.apply(value) != null)
                .collect(Collectors.toMap(idExtractor, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
    }

    private record ReadModelContext(
            Map<Long, List<Long>> deviceIdsByDeliveryId,
            Map<Long, RentalDeviceDO> devicesById,
            List<RentalScheduleDO> schedules
    ) {
    }
}
