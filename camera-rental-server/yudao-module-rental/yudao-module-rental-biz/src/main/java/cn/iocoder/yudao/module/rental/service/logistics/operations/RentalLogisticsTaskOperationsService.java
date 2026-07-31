package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalAsyncProcessingStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryOutboxService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.*;

@Service
public class RentalLogisticsTaskOperationsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter RECONCILE_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Set<String> RETRYABLE_STATUSES = Set.of("FAILED", "DEAD", "RETRY_WAIT");
    private static final int MAX_QUERY_LIMIT = 100;
    private static final int MAX_RECONCILE_LIMIT = 100;

    private final RentalLogisticsOperationsMapper operationsMapper;
    private final RentalDeliveryOutboxMapper outboxMapper;
    private final RentalDeliveryCallbackInboxMapper inboxMapper;
    private final RentalDeliveryOutboxService outboxService;

    public RentalLogisticsTaskOperationsService(RentalLogisticsOperationsMapper operationsMapper,
                                                RentalDeliveryOutboxMapper outboxMapper,
                                                RentalDeliveryCallbackInboxMapper inboxMapper,
                                                RentalDeliveryOutboxService outboxService) {
        this.operationsMapper = operationsMapper;
        this.outboxMapper = outboxMapper;
        this.inboxMapper = inboxMapper;
        this.outboxService = outboxService;
    }

    public List<FailedTaskView> listFailedTasks(String taskType, Integer limit) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        int boundedLimit = bound(limit, 1, MAX_QUERY_LIMIT, 50);
        String normalizedType = taskType == null ? "ALL" : taskType.trim().toUpperCase(Locale.ROOT);
        List<FailedTaskView> result = new ArrayList<>();
        if ("ALL".equals(normalizedType) || "OUTBOX".equals(normalizedType)) {
            operationsMapper.selectFailedOutbox(tenantId, boundedLimit).stream()
                    .map(this::toView).forEach(result::add);
        }
        if ("ALL".equals(normalizedType) || "INBOX".equals(normalizedType)) {
            operationsMapper.selectFailedInbox(tenantId, boundedLimit).stream()
                    .map(this::toView).forEach(result::add);
        }
        if (!Set.of("ALL", "OUTBOX", "INBOX").contains(normalizedType)) {
            throw new RentalLogisticsException("FAILED_TASK_TYPE_INVALID");
        }
        return result.stream()
                .sorted(Comparator.comparing(FailedTaskView::occurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(boundedLimit)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public RetryResult retry(String taskType, Long taskId) {
        if (taskId == null) {
            throw new RentalLogisticsException("FAILED_TASK_ID_REQUIRED");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String normalizedType = taskType == null ? "" : taskType.trim().toUpperCase(Locale.ROOT);
        if ("OUTBOX".equals(normalizedType)) {
            RentalDeliveryOutboxDO task = operationsMapper.selectOutboxForUpdate(tenantId, taskId);
            requireRetryable(task == null ? null : task.getProcessingStatus());
            task.setProcessingStatus(RentalAsyncProcessingStatusEnum.PENDING.name());
            task.setProcessingToken(null);
            task.setLeaseUntil(null);
            task.setNextAttemptAt(null);
            task.setProcessedAt(null);
            task.setLastErrorCode(null);
            task.setLastErrorMessage(null);
            outboxMapper.updateById(task);
            return new RetryResult(true, "OUTBOX_REQUEUED", task.getProcessingStatus());
        }
        if ("INBOX".equals(normalizedType)) {
            RentalDeliveryCallbackInboxDO task = operationsMapper.selectInboxForUpdate(tenantId, taskId);
            requireRetryable(task == null ? null : task.getProcessingStatus());
            task.setProcessingStatus(RentalAsyncProcessingStatusEnum.RECEIVED.name());
            task.setProcessingToken(null);
            task.setLeaseUntil(null);
            task.setNextRetryAt(null);
            task.setProcessedAt(null);
            task.setLastErrorCode(null);
            task.setLastErrorMessage(null);
            inboxMapper.updateById(task);
            return new RetryResult(true, "INBOX_REQUEUED", task.getProcessingStatus());
        }
        throw new RentalLogisticsException("FAILED_TASK_TYPE_INVALID");
    }

    public ReconcileResult reconcile(Integer limit) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        int boundedLimit = bound(limit, 1, MAX_RECONCILE_LIMIT, 20);
        List<Long> deliveryIds = operationsMapper.selectReconcileCandidateIds(tenantId, boundedLimit);
        String bucket = LocalDateTime.now(BUSINESS_ZONE).format(RECONCILE_BUCKET);
        for (Long deliveryId : deliveryIds) {
            outboxService.enqueue(deliveryId, RentalDeliveryOutboxEventTypeEnum.RECONCILE,
                    "manual:" + bucket, "manual reconcile");
        }
        return new ReconcileResult(boundedLimit, deliveryIds.size(), deliveryIds);
    }

    private FailedTaskView toView(RentalDeliveryOutboxDO task) {
        return new FailedTaskView("OUTBOX", task.getId(), task.getDeliveryId(), null, task.getEventType(),
                task.getProcessingStatus(), valueOrZero(task.getRetryCount()), task.getNextAttemptAt(),
                safeCode(task.getLastErrorCode()), safeMessage(task.getLastErrorCode()),
                task.getUpdateTime() == null ? task.getScheduledAt() : task.getUpdateTime());
    }

    private FailedTaskView toView(RentalDeliveryCallbackInboxDO task) {
        return new FailedTaskView("INBOX", task.getId(), task.getDeliveryId(), task.getProviderCode(), null,
                task.getProcessingStatus(), valueOrZero(task.getRetryCount()), task.getNextRetryAt(),
                safeCode(task.getLastErrorCode()), safeMessage(task.getLastErrorCode()),
                task.getUpdateTime() == null ? task.getReceivedAt() : task.getUpdateTime());
    }

    private void requireRetryable(String status) {
        if (status == null) {
            throw new RentalLogisticsException("FAILED_TASK_NOT_FOUND");
        }
        if (!RETRYABLE_STATUSES.contains(status)) {
            throw new RentalLogisticsException("FAILED_TASK_STATE_NOT_RETRYABLE");
        }
    }

    private String safeCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private String safeMessage(String code) {
        String normalized = safeCode(code);
        return normalized == null ? "LOGISTICS_TASK_FAILED" : normalized;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int bound(Integer value, int minimum, int maximum, int defaultValue) {
        int effective = value == null ? defaultValue : value;
        if (effective < minimum || effective > maximum) {
            throw new RentalLogisticsException("LIMIT_OUT_OF_RANGE");
        }
        return effective;
    }
}
