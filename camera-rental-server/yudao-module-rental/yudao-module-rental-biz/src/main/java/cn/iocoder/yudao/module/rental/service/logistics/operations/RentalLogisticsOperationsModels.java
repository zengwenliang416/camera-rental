package cn.iocoder.yudao.module.rental.service.logistics.operations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class RentalLogisticsOperationsModels {

    private RentalLogisticsOperationsModels() {
    }

    public enum SecretAction {
        KEEP,
        REPLACE,
        CLEAR
    }

    public record ProviderConfigCommand(
            String providerCode,
            Boolean enabled,
            Boolean queryEnabled,
            Boolean subscribeEnabled,
            SecretAction callbackSecretAction,
            String callbackSecret,
            String callbackBaseUrl,
            Integer minimumQueryIntervalSeconds,
            String resultVersion
    ) {
    }

    public record ProviderConfigView(
            String providerCode,
            boolean enabled,
            boolean queryEnabled,
            boolean subscribeEnabled,
            boolean callbackSecretConfigured,
            String maskedCallbackSecret,
            String callbackBaseUrl,
            int minimumQueryIntervalSeconds,
            String resultVersion,
            String configStatus,
            LocalDateTime lastVerifiedAt,
            List<ProviderCredentialView> credentials
    ) {
        public ProviderConfigView {
            credentials = credentials == null ? List.of() : List.copyOf(credentials);
        }
    }

    public record ProviderCredentialCommand(
            Long id,
            String providerCode,
            String credentialName,
            Boolean enabled,
            Integer sortOrder,
            SecretAction customerCodeAction,
            String customerCode,
            SecretAction apiKeyAction,
            String apiKey
    ) {
    }

    public record ProviderCredentialView(
            Long id,
            String providerCode,
            String credentialName,
            boolean enabled,
            int sortOrder,
            boolean customerCodeConfigured,
            String maskedCustomerCode,
            boolean apiKeyConfigured,
            String maskedApiKey,
            String configStatus,
            LocalDateTime lastVerifiedAt
    ) {
    }

    public record ProviderVerifyResult(boolean valid, String reason, LocalDateTime verifiedAt) {
    }

    public record CarrierMappingCommand(
            Long id,
            String sourceType,
            String sourceCarrierCode,
            String canonicalCarrierCode,
            String displayName,
            String providerCode,
            String providerCarrierCode,
            String phoneRequirement,
            String status
    ) {
    }

    public record CarrierMappingView(
            Long id,
            String sourceType,
            String sourceCarrierCode,
            String canonicalCarrierCode,
            String displayName,
            String providerCode,
            String providerCarrierCode,
            String phoneRequirement,
            String status
    ) {
    }

    public record FailedTaskView(
            String taskType,
            Long id,
            Long deliveryId,
            String providerCode,
            String eventType,
            String processingStatus,
            int retryCount,
            LocalDateTime nextAttemptAt,
            String errorCode,
            String safeErrorMessage,
            LocalDateTime occurredAt
    ) {
    }

    public record RetryResult(boolean accepted, String reason, String processingStatus) {
    }

    public record ReconcileResult(int requestedLimit, int enqueuedCount, List<Long> deliveryIds) {
        public ReconcileResult {
            deliveryIds = deliveryIds == null ? List.of() : List.copyOf(deliveryIds);
        }
    }

    public record MetricsView(
            long deliveryCount,
            Map<String, Long> deliveryStatusCounts,
            Map<String, Long> outboxStatusCounts,
            Map<String, Long> inboxStatusCounts,
            long staleDeliveryCount,
            long failedOutboxCount,
            long failedInboxCount,
            long retriedOutboxCount,
            long retriedInboxCount,
            long averageOutboxDelaySeconds,
            LocalDateTime lastOutboxSuccessAt,
            LocalDateTime lastInboxSuccessAt
    ) {
        public MetricsView {
            deliveryStatusCounts = deliveryStatusCounts == null ? Map.of() : Map.copyOf(deliveryStatusCounts);
            outboxStatusCounts = outboxStatusCounts == null ? Map.of() : Map.copyOf(outboxStatusCounts);
            inboxStatusCounts = inboxStatusCounts == null ? Map.of() : Map.copyOf(inboxStatusCounts);
        }
    }

    public record BackfillCommand(
            boolean dryRun,
            int limit,
            boolean enqueueProviderTasks,
            LocalDate consignDateStart,
            LocalDate consignDateEnd
    ) {
        public BackfillCommand(boolean dryRun, int limit, boolean enqueueProviderTasks) {
            this(dryRun, limit, enqueueProviderTasks, null, null);
        }
    }

    public record BackfillItem(
            Long shipmentId,
            Long deliveryId,
            String maskedWaybillNo,
            String status,
            String reason
    ) {
    }

    public record BackfillResult(
            boolean dryRun,
            int requestedLimit,
            int candidateCount,
            int distinctWaybillCount,
            int createdOrReusedCount,
            int skippedCount,
            boolean providerTasksEnqueued,
            String providerTaskReason,
            List<BackfillItem> items
    ) {
        public BackfillResult {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record CleanupCommand(boolean dryRun, int retentionDays, int limit) {
    }

    public record CleanupResult(
            boolean dryRun,
            int retentionDays,
            int limit,
            int traceCount,
            int inboxCount,
            int outboxCount
    ) {
        public int totalCount() {
            return traceCount + inboxCount + outboxCount;
        }
    }
}
