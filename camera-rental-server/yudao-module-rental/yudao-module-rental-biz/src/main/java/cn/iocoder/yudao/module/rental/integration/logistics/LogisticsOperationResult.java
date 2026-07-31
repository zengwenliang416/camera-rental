package cn.iocoder.yudao.module.rental.integration.logistics;

public record LogisticsOperationResult(
        boolean successful,
        boolean retryable,
        String providerTaskId,
        String safeCode,
        LogisticsTrackingSnapshot snapshot
) {

    public static LogisticsOperationResult success(String providerTaskId, LogisticsTrackingSnapshot snapshot) {
        return new LogisticsOperationResult(true, false, providerTaskId, null, snapshot);
    }

    public static LogisticsOperationResult failure(String safeCode, boolean retryable) {
        return new LogisticsOperationResult(false, retryable, null, safeCode, null);
    }

    public static LogisticsOperationResult skipped(String safeCode) {
        return new LogisticsOperationResult(true, false, null, safeCode, null);
    }
}
