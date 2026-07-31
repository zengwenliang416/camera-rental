package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalAsyncProcessingStatusEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class RentalDeliveryInboxCompletionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final RentalDeliveryCallbackInboxMapper inboxMapper;
    private final RentalTrackingSnapshotService snapshotService;
    private final RentalAsyncRetryPolicy retryPolicy;

    public RentalDeliveryInboxCompletionService(RentalDeliveryCallbackInboxMapper inboxMapper,
                                                RentalTrackingSnapshotService snapshotService,
                                                RentalAsyncRetryPolicy retryPolicy) {
        this.inboxMapper = inboxMapper;
        this.snapshotService = snapshotService;
        this.retryPolicy = retryPolicy;
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(RentalInboxWorkItem work, LogisticsOperationResult result) {
        RentalDeliveryCallbackInboxDO inbox = inboxMapper.selectById(work.inboxId());
        if (inbox == null || !Objects.equals(work.processingToken(), inbox.getProcessingToken())
                || !RentalAsyncProcessingStatusEnum.PROCESSING.name().equals(inbox.getProcessingStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (result.successful()) {
            if (result.snapshot() != null) {
                snapshotService.apply(work.deliveryId(), result.snapshot());
            }
            inbox.setProcessingStatus(RentalAsyncProcessingStatusEnum.SUCCEEDED.name());
            inbox.setProcessedAt(now);
            inbox.setLastErrorCode(null);
            inbox.setLastErrorMessage(null);
        } else {
            int retryCount = (inbox.getRetryCount() == null ? 0 : inbox.getRetryCount()) + 1;
            boolean retry = result.retryable() && !retryPolicy.exhausted(retryCount);
            inbox.setRetryCount(retryCount);
            inbox.setProcessingStatus(retry ? RentalAsyncProcessingStatusEnum.RETRY_WAIT.name()
                    : RentalAsyncProcessingStatusEnum.DEAD.name());
            inbox.setNextRetryAt(retry ? now.plus(retryPolicy.delay(retryCount)) : null);
            inbox.setLastErrorCode(result.safeCode());
            inbox.setLastErrorMessage(result.safeCode());
        }
        inbox.setProcessingToken(null);
        inbox.setLeaseUntil(null);
        inboxMapper.updateById(inbox);
    }
}
