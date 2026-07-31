package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalLogisticsRiskServiceTest {

    private final RentalLogisticsRiskService service = new RentalLogisticsRiskService();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 31, 12, 0);

    @Test
    void calculatesStableOutboundAndProviderRisksWithoutChangingDeviceState() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(10L)
                .billableStartDate(LocalDate.of(2026, 7, 30))
                .billableEndDate(LocalDate.of(2026, 8, 2))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 5))
                .build();
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(20L)
                .rentalOrderId(10L)
                .direction("OUTBOUND")
                .mappingStatus("MAPPING_REQUIRED")
                .subscribeStatus("FAILED")
                .trackingStatus("INFO_RECEIVED")
                .lastSyncedAt(now.minusHours(30))
                .build();

        List<RentalLogisticsRisk> risks = service.evaluate(order, List.of(delivery),
                Map.of(20L, List.of(101L)), List.of(), now);

        assertEquals(List.of(
                        "OUTBOUND_NOT_PICKED_UP",
                        "TRACKING_STALE",
                        "MAPPING_REQUIRED",
                        "SUBSCRIPTION_FAILED"),
                risks.stream().map(RentalLogisticsRisk::code).toList());
        assertTrue(risks.stream().allMatch(risk -> risk.affectedDeviceIds().equals(List.of(101L))));
    }

    @Test
    void detectsMissingReturnAndUpcomingSchedulePressure() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(10L)
                .billableEndDate(LocalDate.of(2026, 7, 29))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 31))
                .build();
        RentalDeliveryDO outbound = RentalDeliveryDO.builder()
                .id(20L)
                .rentalOrderId(10L)
                .direction("OUTBOUND")
                .trackingStatus("DELIVERED")
                .build();
        RentalScheduleDO nextSchedule = RentalScheduleDO.builder()
                .deviceId(101L)
                .rentalOrderId(11L)
                .status("EFFECTIVE")
                .occupyStartDate(LocalDate.of(2026, 8, 1))
                .build();

        List<RentalLogisticsRisk> risks = service.evaluate(order, List.of(outbound),
                Map.of(20L, List.of(101L)), List.of(nextSchedule), now);

        assertEquals(List.of("RETURN_NOT_SHIPPED"),
                risks.stream().map(RentalLogisticsRisk::code).toList());
        assertEquals("HIGH", risks.get(0).severity());
    }

    @Test
    void flagsDelayedReturnAndLogisticsException() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(10L)
                .billableEndDate(LocalDate.of(2026, 7, 28))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 30))
                .build();
        RentalDeliveryDO returnedPackage = RentalDeliveryDO.builder()
                .id(21L)
                .rentalOrderId(10L)
                .direction("RETURN")
                .trackingStatus("EXCEPTION")
                .lastSyncedAt(now)
                .build();

        List<RentalLogisticsRisk> risks = service.evaluate(order, List.of(returnedPackage),
                Map.of(21L, List.of(101L)), List.of(), now);

        assertEquals(List.of("RETURN_DELIVERY_DELAY", "LOGISTICS_EXCEPTION"),
                risks.stream().map(RentalLogisticsRisk::code).toList());
    }
}
