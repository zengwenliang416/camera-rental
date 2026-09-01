package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelReconciliationRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelReconciliationRunMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_RECONCILIATION_ACTIVE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_RECONCILIATION_NOT_EXISTS;

@Service
public class RentalChannelReconciliationRunService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalChannelReconciliationRunMapper runMapper;

    public RentalChannelReconciliationRunService(
            RentalChannelReconciliationRunMapper runMapper) {
        this.runMapper = runMapper;
    }

    public Long createRuleChangeRun(Long productRuleId, Long shopId, String xianyuItemId) {
        RentalChannelReconciliationRunDO run = RentalChannelReconciliationRunDO.builder()
                .productRuleId(productRuleId)
                .shopId(shopId)
                .xianyuItemId(xianyuItemId)
                .triggerType("RULE_CHANGE")
                .status("PENDING")
                .scannedCount(0)
                .skippedCount(0)
                .createdCount(0)
                .updatedCount(0)
                .unchangedCount(0)
                .conflictCount(0)
                .failedCount(0)
                .reviewRequiredCount(0)
                .build();
        run.setTenantId(TenantContextHolder.getRequiredTenantId());
        runMapper.insert(run);
        return run.getId();
    }

    public RentalChannelReconciliationRunDO get(Long runId) {
        RentalChannelReconciliationRunDO run = runMapper.selectByTenantIdAndId(
                TenantContextHolder.getRequiredTenantId(), runId);
        if (run == null) {
            throw exception(RENTAL_CHANNEL_RECONCILIATION_NOT_EXISTS);
        }
        return run;
    }

    public void assertNoActiveRuleRun(Long productRuleId) {
        if (runMapper.existsActiveByTenantIdAndProductRuleId(
                TenantContextHolder.getRequiredTenantId(), productRuleId)) {
            throw exception(RENTAL_CHANNEL_RECONCILIATION_ACTIVE);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markRunning(Long runId) {
        RentalChannelReconciliationRunDO run = get(runId);
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now(BUSINESS_ZONE));
        run.setFinishedAt(null);
        run.setLastErrorCode(null);
        runMapper.updateById(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void complete(Long runId, RentalChannelReconciliationCounters counters) {
        RentalChannelReconciliationRunDO run = get(runId);
        applyCounters(run, counters);
        run.setStatus(counters.failed() > 0 ? "COMPLETED_WITH_ERRORS" : "SUCCEEDED");
        run.setFinishedAt(LocalDateTime.now(BUSINESS_ZONE));
        runMapper.updateById(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void fail(Long runId, RentalChannelReconciliationCounters counters, String errorCode) {
        RentalChannelReconciliationRunDO run = get(runId);
        applyCounters(run, counters);
        run.setStatus("FAILED");
        run.setLastErrorCode(errorCode);
        run.setFinishedAt(LocalDateTime.now(BUSINESS_ZONE));
        runMapper.updateById(run);
    }

    private static void applyCounters(RentalChannelReconciliationRunDO run,
                                      RentalChannelReconciliationCounters counters) {
        run.setScannedCount(counters.scanned());
        run.setSkippedCount(counters.skipped());
        run.setCreatedCount(counters.created());
        run.setUpdatedCount(counters.updated());
        run.setUnchangedCount(counters.unchanged());
        run.setConflictCount(counters.conflict());
        run.setFailedCount(counters.failed());
        run.setReviewRequiredCount(counters.reviewRequired());
    }

}
