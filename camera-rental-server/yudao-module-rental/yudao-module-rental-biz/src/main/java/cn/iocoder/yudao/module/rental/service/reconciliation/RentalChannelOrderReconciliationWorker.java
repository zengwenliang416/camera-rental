package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@Slf4j
public class RentalChannelOrderReconciliationWorker {

    static final int BATCH_SIZE = 500;

    private final XianyuOrderMapper orderMapper;
    private final RentalChannelOrderReconciliationService reconciliationService;
    private final RentalChannelReconciliationRunService runService;

    public RentalChannelOrderReconciliationWorker(
            XianyuOrderMapper orderMapper,
            RentalChannelOrderReconciliationService reconciliationService,
            RentalChannelReconciliationRunService runService) {
        this.orderMapper = orderMapper;
        this.reconciliationService = reconciliationService;
        this.runService = runService;
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void onRequested(RentalChannelOrderReconciliationRequestedEvent event) {
        TenantUtils.execute(event.tenantId(), () -> processTracked(event));
    }

    void process(RentalChannelOrderReconciliationRequestedEvent event) {
        processCandidates(event, new CounterProgress());
    }

    private void processTracked(RentalChannelOrderReconciliationRequestedEvent event) {
        if (event.reconciliationRunId() == null) {
            processCandidates(event, new CounterProgress());
            return;
        }
        CounterProgress progress = new CounterProgress();
        try {
            runService.markRunning(event.reconciliationRunId());
            processCandidates(event, progress);
            runService.complete(event.reconciliationRunId(), progress.counters);
        } catch (RuntimeException exception) {
            runService.fail(event.reconciliationRunId(), progress.counters,
                    exception.getClass().getSimpleName());
            log.warn("[xianyu][reconcile-worker] tracked reconciliation failed runId={} code={}",
                    event.reconciliationRunId(), exception.getClass().getSimpleName());
        }
    }

    private void processCandidates(
            RentalChannelOrderReconciliationRequestedEvent event,
            CounterProgress progress) {
        Long afterId = null;
        while (true) {
            List<Long> candidateIds = selectCandidateIds(event, afterId);
            if (candidateIds == null || candidateIds.isEmpty()) {
                return;
            }
            for (Long candidateId : candidateIds) {
                progress.counters = reconcileSafely(candidateId, progress.counters);
            }
            Long nextAfterId = candidateIds.get(candidateIds.size() - 1);
            if (afterId != null && nextAfterId <= afterId) {
                log.error("[xianyu][reconcile-worker] non-advancing cursor tenantId={} scope={} afterId={} nextAfterId={}",
                        event.tenantId(), event.scope(), afterId, nextAfterId);
                throw new IllegalStateException("Reconciliation cursor did not advance");
            }
            afterId = nextAfterId;
            if (candidateIds.size() < BATCH_SIZE) {
                return;
            }
        }
    }

    private List<Long> selectCandidateIds(
            RentalChannelOrderReconciliationRequestedEvent event, Long afterId) {
        return switch (event.scope()) {
            case ITEM -> orderMapper.selectMutableReconciliationCandidateIdsByItem(
                    event.tenantId(), event.shopId(), event.xianyuItemId(), afterId, BATCH_SIZE);
            case PRODUCT -> orderMapper.selectMutableReconciliationCandidateIdsByProduct(
                    event.tenantId(), event.shopId(), event.xgjProductId(), afterId, BATCH_SIZE);
            case PRODUCT_SKUS -> orderMapper.selectMutableReconciliationCandidateIdsByProductAndSkus(
                    event.tenantId(), event.shopId(), event.xgjProductId(),
                    event.xgjSkuIds(), afterId, BATCH_SIZE);
        };
    }

    private RentalChannelReconciliationCounters reconcileSafely(
            Long channelOrderId,
            RentalChannelReconciliationCounters counters) {
        try {
            return counters.record(reconciliationService.reconcile(channelOrderId));
        } catch (RuntimeException exception) {
            log.warn("[xianyu][reconcile-worker] order reconciliation failed channelOrderId={} code={}",
                    channelOrderId, exception.getClass().getSimpleName());
            return counters.recordFailure();
        }
    }

    private static final class CounterProgress {

        private RentalChannelReconciliationCounters counters =
                RentalChannelReconciliationCounters.empty();

    }

}
