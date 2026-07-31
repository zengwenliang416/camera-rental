package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalAsyncProcessingStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class RentalDeliveryOutboxCompletionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalDeliveryOutboxMapper outboxMapper;
    private final RentalDeliveryMapper deliveryMapper;
    private final RentalTrackingSnapshotService snapshotService;
    private final RentalAsyncRetryPolicy retryPolicy;
    private final SensitiveValueRedactor redactor;

    public RentalDeliveryOutboxCompletionService(RentalDeliveryOutboxMapper outboxMapper,
                                                 RentalDeliveryMapper deliveryMapper,
                                                 RentalTrackingSnapshotService snapshotService,
                                                 RentalAsyncRetryPolicy retryPolicy,
                                                 SensitiveValueRedactor redactor) {
        this.outboxMapper = outboxMapper;
        this.deliveryMapper = deliveryMapper;
        this.snapshotService = snapshotService;
        this.retryPolicy = retryPolicy;
        this.redactor = redactor;
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(RentalOutboxWorkItem work, LogisticsOperationResult result) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        RentalDeliveryOutboxDO task = outboxMapper.selectById(work.outboxId());
        if (task == null || !Objects.equals(work.processingToken(), task.getProcessingToken())
                || !RentalAsyncProcessingStatusEnum.PROCESSING.name().equals(task.getProcessingStatus())) {
            return;
        }
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndIdForUpdate(work.tenantId(), work.deliveryId());
        if (result.successful()) {
            if (result.snapshot() != null) {
                snapshotService.apply(work.deliveryId(), result.snapshot());
            }
            task.setProcessingStatus(RentalAsyncProcessingStatusEnum.SUCCEEDED.name());
            task.setProcessedAt(now);
            task.setLastErrorCode(result.safeCode());
            task.setLastErrorMessage(redactor.redact(result.safeCode()));
            if (delivery != null) {
                if (result.safeCode() == null) {
                    markDeliverySuccess(delivery, work.eventType());
                } else {
                    markDeliverySkipped(delivery, work.eventType(), result.safeCode());
                }
            }
        } else {
            int retryCount = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
            boolean retry = result.retryable() && !retryPolicy.exhausted(retryCount);
            task.setRetryCount(retryCount);
            task.setProcessingStatus(retry ? RentalAsyncProcessingStatusEnum.RETRY_WAIT.name()
                    : RentalAsyncProcessingStatusEnum.DEAD.name());
            task.setNextAttemptAt(retry ? now.plus(retryPolicy.delay(retryCount)) : null);
            task.setLastErrorCode(result.safeCode());
            task.setLastErrorMessage(redactor.redact(result.safeCode()));
            if (delivery != null) {
                delivery.setLastErrorCode(result.safeCode());
                delivery.setLastErrorMessage(redactor.redact(result.safeCode()));
                if (work.eventType() == RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE) {
                    delivery.setSubscribeStatus(retry ? "RETRY_WAIT" : "FAILED");
                } else {
                    delivery.setQueryStatus(retry ? "RETRY_WAIT" : "FAILED");
                }
            }
        }
        task.setProcessingToken(null);
        task.setLeaseUntil(null);
        outboxMapper.updateById(task);
        if (delivery != null) {
            deliveryMapper.updateById(delivery);
        }
    }

    private void markDeliverySuccess(RentalDeliveryDO delivery, RentalDeliveryOutboxEventTypeEnum eventType) {
        delivery.setLastErrorCode(null);
        delivery.setLastErrorMessage(null);
        if (eventType == RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE) {
            delivery.setSubscribeStatus("SUBSCRIBED");
        } else {
            delivery.setQueryStatus("READY");
        }
    }

    private void markDeliverySkipped(RentalDeliveryDO delivery, RentalDeliveryOutboxEventTypeEnum eventType,
                                     String safeCode) {
        delivery.setLastErrorCode(safeCode);
        delivery.setLastErrorMessage(safeCode);
        if (eventType == RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE) {
            delivery.setSubscribeStatus(safeCode);
        } else {
            delivery.setQueryStatus(safeCode);
        }
    }
}
