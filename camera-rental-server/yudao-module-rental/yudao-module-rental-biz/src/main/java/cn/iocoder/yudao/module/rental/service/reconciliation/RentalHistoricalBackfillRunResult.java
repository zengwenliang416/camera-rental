package cn.iocoder.yudao.module.rental.service.reconciliation;

public record RentalHistoricalBackfillRunResult(Long runId,
                                                boolean dryRun,
                                                String status,
                                                long startAfterId,
                                                long endIdInclusive,
                                                long cursorAfterId,
                                                int batchSize,
                                                int resumeCount,
                                                int scannedCount,
                                                int skippedCount,
                                                int createdCount,
                                                int updatedCount,
                                                int unchangedCount,
                                                int conflictCount,
                                                int failedCount,
                                                int reviewRequiredCount,
                                                Long lastFailedOrderId,
                                                String lastErrorCode) {
}

