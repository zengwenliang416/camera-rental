package cn.iocoder.yudao.module.rental.service.reconciliation;

public record RentalFulfillmentUpdateResult(boolean planApplied,
                                            boolean modelApplied,
                                            boolean reviewRequired,
                                            String reasonCode) {

    public static RentalFulfillmentUpdateResult applied(boolean planApplied, boolean modelApplied) {
        return new RentalFulfillmentUpdateResult(planApplied, modelApplied, false, null);
    }

    public static RentalFulfillmentUpdateResult review(String reasonCode) {
        return new RentalFulfillmentUpdateResult(false, false, true, reasonCode);
    }

}
