package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RentalLogisticsRiskService {

    private static final long STALE_HOURS = 24;
    private static final long UPCOMING_SCHEDULE_DAYS = 2;
    private static final Set<String> PRE_PICKUP_STATUSES = Set.of("CREATED", "INFO_RECEIVED");
    private static final Set<String> TERMINAL_STATUSES = Set.of("DELIVERED", "RETURNED");

    public List<RentalLogisticsRisk> evaluate(RentalOrderDO order,
                                              List<RentalDeliveryDO> deliveries,
                                              Map<Long, List<Long>> deviceIdsByDeliveryId,
                                              List<RentalScheduleDO> schedules,
                                              LocalDateTime now) {
        if (order == null || now == null) {
            return List.of();
        }
        List<RentalDeliveryDO> safeDeliveries = deliveries == null ? List.of() : deliveries;
        Map<Long, List<Long>> safeDeviceIds = deviceIdsByDeliveryId == null ? Map.of() : deviceIdsByDeliveryId;
        List<RentalScheduleDO> safeSchedules = schedules == null ? List.of() : schedules;
        List<RentalLogisticsRisk> risks = new ArrayList<>();

        List<RentalDeliveryDO> outbound = byDirection(safeDeliveries, "OUTBOUND");
        List<RentalDeliveryDO> returns = byDirection(safeDeliveries, "RETURN");
        LocalDate today = now.toLocalDate();

        if (isDue(order.getBillableStartDate(), today) && !outbound.isEmpty()) {
            if (outbound.stream().allMatch(delivery -> PRE_PICKUP_STATUSES.contains(delivery.getTrackingStatus()))) {
                risks.add(risk("OUTBOUND_NOT_PICKED_UP",
                        order.getBillableStartDate().isBefore(today) ? "HIGH" : "MEDIUM",
                        "Outbound package has not been picked up before the rental period.",
                        "Confirm carrier pickup or arrange shipment.",
                        affectedDevices(outbound, safeDeviceIds)));
            } else if (outbound.stream().noneMatch(this::isTerminal)) {
                risks.add(risk("OUTBOUND_DELIVERY_DELAY", "HIGH",
                        "Outbound delivery may miss the rental period.",
                        "Review the latest trace and prepare a delivery contingency.",
                        affectedDevices(outbound, safeDeviceIds)));
            }
        }

        if (isPast(order.getBillableEndDate(), today)) {
            if (returns.isEmpty()) {
                List<Long> affected = affectedDevices(outbound, safeDeviceIds);
                boolean schedulePressure = hasUpcomingSchedule(affected, order.getId(), safeSchedules, today);
                boolean occupancyExpired = order.getOccupyEndDateExclusive() != null
                        && !order.getOccupyEndDateExclusive().isAfter(today);
                risks.add(risk("RETURN_NOT_SHIPPED", schedulePressure || occupancyExpired ? "HIGH" : "MEDIUM",
                        "No return package has been recorded after the rental period.",
                        "Contact the renter and record the return shipment.",
                        affected));
            } else if (order.getOccupyEndDateExclusive() != null
                    && !order.getOccupyEndDateExclusive().isAfter(today)
                    && returns.stream().noneMatch(this::isTerminal)) {
                risks.add(risk("RETURN_DELIVERY_DELAY", "HIGH",
                        "Return delivery has exceeded the occupied period.",
                        "Review the return trace and protect the next booking.",
                        affectedDevices(returns, safeDeviceIds)));
            }
        }

        List<RentalDeliveryDO> stale = safeDeliveries.stream()
                .filter(delivery -> !isTerminal(delivery))
                .filter(delivery -> delivery.getLastSyncedAt() != null)
                .filter(delivery -> delivery.getLastSyncedAt().isBefore(now.minusHours(STALE_HOURS)))
                .toList();
        if (!stale.isEmpty()) {
            risks.add(risk("TRACKING_STALE", "MEDIUM",
                    "Tracking has not been updated within the expected interval.",
                    "Request a local asynchronous refresh.",
                    affectedDevices(stale, safeDeviceIds)));
        }

        List<RentalDeliveryDO> exceptions = safeDeliveries.stream()
                .filter(delivery -> "EXCEPTION".equals(delivery.getTrackingStatus()))
                .toList();
        if (!exceptions.isEmpty()) {
            risks.add(risk("LOGISTICS_EXCEPTION", "HIGH",
                    "The carrier reported a logistics exception.",
                    "Review the stored trace and contact the carrier if needed.",
                    affectedDevices(exceptions, safeDeviceIds)));
        }

        List<RentalDeliveryDO> mappingRequired = safeDeliveries.stream()
                .filter(delivery -> "MAPPING_REQUIRED".equals(delivery.getMappingStatus()))
                .toList();
        if (!mappingRequired.isEmpty()) {
            risks.add(risk("MAPPING_REQUIRED", "MEDIUM",
                    "Carrier mapping is required before tracking can continue.",
                    "Configure the tenant carrier mapping.",
                    affectedDevices(mappingRequired, safeDeviceIds)));
        }

        List<RentalDeliveryDO> subscriptionFailed = safeDeliveries.stream()
                .filter(delivery -> "FAILED".equals(delivery.getSubscribeStatus()))
                .toList();
        if (!subscriptionFailed.isEmpty()) {
            risks.add(risk("SUBSCRIPTION_FAILED", "MEDIUM",
                    "Carrier tracking subscription failed.",
                    "Review the safe task status and retry asynchronously.",
                    affectedDevices(subscriptionFailed, safeDeviceIds)));
        }
        return List.copyOf(risks);
    }

    private List<RentalDeliveryDO> byDirection(List<RentalDeliveryDO> deliveries, String direction) {
        return deliveries.stream()
                .filter(delivery -> direction.equals(delivery.getDirection()))
                .sorted(Comparator.comparing(RentalDeliveryDO::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private boolean isDue(LocalDate date, LocalDate today) {
        return date != null && !date.isAfter(today);
    }

    private boolean isPast(LocalDate date, LocalDate today) {
        return date != null && date.isBefore(today);
    }

    private boolean isTerminal(RentalDeliveryDO delivery) {
        return TERMINAL_STATUSES.contains(delivery.getTrackingStatus());
    }

    private boolean hasUpcomingSchedule(List<Long> deviceIds, Long currentOrderId,
                                        List<RentalScheduleDO> schedules, LocalDate today) {
        if (deviceIds.isEmpty()) {
            return false;
        }
        LocalDate pressureEnd = today.plusDays(UPCOMING_SCHEDULE_DAYS);
        Set<Long> affected = Set.copyOf(deviceIds);
        return schedules.stream()
                .filter(schedule -> "EFFECTIVE".equals(schedule.getStatus()))
                .filter(schedule -> !Objects.equals(currentOrderId, schedule.getRentalOrderId()))
                .filter(schedule -> affected.contains(schedule.getDeviceId()))
                .map(RentalScheduleDO::getOccupyStartDate)
                .filter(Objects::nonNull)
                .anyMatch(start -> !start.isBefore(today) && !start.isAfter(pressureEnd));
    }

    private List<Long> affectedDevices(Collection<RentalDeliveryDO> deliveries,
                                       Map<Long, List<Long>> deviceIdsByDeliveryId) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        deliveries.stream()
                .map(RentalDeliveryDO::getId)
                .filter(Objects::nonNull)
                .sorted()
                .map(deviceIdsByDeliveryId::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .sorted()
                .forEach(result::add);
        return List.copyOf(result);
    }

    private RentalLogisticsRisk risk(String code, String severity, String safeMessage,
                                     String nextAction, List<Long> affectedDeviceIds) {
        return new RentalLogisticsRisk(code, severity, safeMessage, nextAction, affectedDeviceIds);
    }
}
