package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RentalOrderPreparationPolicyTest {

    private final RentalOrderPreparationPolicy policy = new RentalOrderPreparationPolicy();

    @Test
    void modelGapHasPriorityOverRemarkGap() {
        RentalOrderPreparationDecision decision = policy.evaluate(
                XianyuOrderDO.builder().rentalPeriodStatus("PENDING")
                        .rentalPeriodReasonCode("MISSING_REMARK").build(),
                RentalOrderItemDO.builder().build(),
                "SKU_MODEL_NOT_CONFIGURED");

        assertEquals("WAITING_MODEL", decision.status());
        assertEquals("SKU_MODEL_NOT_CONFIGURED", decision.reasonCode());
    }

    @Test
    void requiresCompleteReadySnapshotBeforeAssignment() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(1L).preparationStatus("WAITING_REMARK")
                .preparationReasonCode("MISSING_REMARK").build();
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .rentalOrderId(1L).equipmentModelCode("A7M4").build();

        RentalDeviceAssignmentException ex = assertThrows(
                RentalDeviceAssignmentException.class, () -> policy.requireReady(order, item));

        assertEquals(RentalDeviceAssignmentException.Code.ORDER_NOT_READY, ex.getCode());
        assertEquals("MISSING_REMARK", ex.getMessage());
    }

    @Test
    void preservesReadyStateWhenLatestRemarkFailsAfterAnEffectivePlan() {
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .equipmentModelCode("A7M4")
                .billableStartDate(LocalDate.of(2026, 7, 25))
                .billableEndDate(LocalDate.of(2026, 7, 27))
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 28))
                .build();

        RentalOrderPreparationDecision decision = policy.evaluate(
                XianyuOrderDO.builder()
                        .rentalPeriodStatus("PENDING")
                        .rentalPeriodReasonCode("MISSING_RETURN_DATE")
                        .build(),
                item,
                null);

        assertEquals("READY", decision.status());
    }

    @Test
    void acceptsOnlyCompleteReadyOrder() {
        RentalOrderDO order = RentalOrderDO.builder()
                .id(1L).preparationStatus("READY").build();
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .rentalOrderId(1L)
                .equipmentModelCode("A7M4")
                .billableStartDate(LocalDate.of(2026, 7, 25))
                .billableEndDate(LocalDate.of(2026, 7, 27))
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 28))
                .build();

        policy.requireReady(order, item);
    }

}
