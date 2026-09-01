package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;

import java.util.Objects;

public record RentalRemarkPlanUpdate(SellerRemarkRentalPeriod previousEffectivePlan,
                                     SellerRemarkRentalPeriod candidatePlan,
                                     RentalRemarkPlanChangeType changeType) {

    public RentalRemarkPlanUpdate {
        Objects.requireNonNull(candidatePlan, "candidatePlan");
        Objects.requireNonNull(changeType, "changeType");
    }

}
