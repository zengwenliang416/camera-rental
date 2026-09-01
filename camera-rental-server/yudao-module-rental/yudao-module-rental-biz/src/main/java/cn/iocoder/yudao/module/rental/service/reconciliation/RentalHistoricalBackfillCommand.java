package cn.iocoder.yudao.module.rental.service.reconciliation;

public record RentalHistoricalBackfillCommand(long startAfterId,
                                              long endIdInclusive,
                                              int batchSize,
                                              int maxBatches,
                                              boolean dryRun,
                                              String confirmation) {
}

