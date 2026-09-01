package cn.iocoder.yudao.module.rental.service.reconciliation;

public record RentalOrderPreparationDecision(String status, String reasonCode) {

    public static RentalOrderPreparationDecision ready() {
        return new RentalOrderPreparationDecision("READY", null);
    }

    public static RentalOrderPreparationDecision waitingModel(String reasonCode) {
        return new RentalOrderPreparationDecision("WAITING_MODEL", reasonCode);
    }

    public static RentalOrderPreparationDecision waitingRemark(String reasonCode) {
        return new RentalOrderPreparationDecision("WAITING_REMARK", reasonCode);
    }

}
