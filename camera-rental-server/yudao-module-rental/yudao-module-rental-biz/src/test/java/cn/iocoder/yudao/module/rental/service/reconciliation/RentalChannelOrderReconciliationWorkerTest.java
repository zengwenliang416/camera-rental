package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalChannelOrderReconciliationWorkerTest {

    @Mock
    private XianyuOrderMapper orderMapper;
    @Mock
    private RentalChannelOrderReconciliationService reconciliationService;
    @Mock
    private RentalChannelReconciliationRunService runService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void itemScopeUsesAscendingCursorUntilEveryBatchIsProcessed() {
        RentalChannelOrderReconciliationWorker worker =
                new RentalChannelOrderReconciliationWorker(
                        orderMapper, reconciliationService, runService);
        List<Long> firstBatch = LongStream.rangeClosed(1, 500).boxed().toList();
        when(orderMapper.selectMutableReconciliationCandidateIdsByItem(
                9L, 7L, "item-1", null, 500)).thenReturn(firstBatch);
        when(orderMapper.selectMutableReconciliationCandidateIdsByItem(
                9L, 7L, "item-1", 500L, 500)).thenReturn(List.of(501L, 502L));

        worker.process(RentalChannelOrderReconciliationRequestedEvent.forItem(
                9L, 7L, "item-1"));

        verify(reconciliationService).reconcile(1L);
        verify(reconciliationService).reconcile(500L);
        verify(reconciliationService).reconcile(501L);
        verify(reconciliationService).reconcile(502L);
        verify(orderMapper).selectMutableReconciliationCandidateIdsByItem(
                9L, 7L, "item-1", 500L, 500);
    }

    @Test
    void oneOrderFailureDoesNotBlockLaterCandidates() {
        RentalChannelOrderReconciliationWorker worker =
                new RentalChannelOrderReconciliationWorker(
                        orderMapper, reconciliationService, runService);
        when(orderMapper.selectMutableReconciliationCandidateIdsByProduct(
                9L, 7L, "product-1", null, 500)).thenReturn(List.of(10L, 11L, 12L));
        doAnswer(invocation -> {
            if (invocation.getArgument(0, Long.class).equals(11L)) {
                throw new IllegalStateException("one bad order");
            }
            return null;
        }).when(reconciliationService).reconcile(anyLong());

        worker.process(RentalChannelOrderReconciliationRequestedEvent.forProduct(
                9L, 7L, "product-1"));

        verify(reconciliationService).reconcile(10L);
        verify(reconciliationService).reconcile(11L);
        verify(reconciliationService).reconcile(12L);
    }

    @Test
    void skuScopeKeepsExactShopProductAndSkuSet() {
        RentalChannelOrderReconciliationWorker worker =
                new RentalChannelOrderReconciliationWorker(
                        orderMapper, reconciliationService, runService);
        when(orderMapper.selectMutableReconciliationCandidateIdsByProductAndSkus(
                9L, 7L, "product-1", List.of("sku-1", "sku-2"), null, 500))
                .thenReturn(List.of());

        worker.process(RentalChannelOrderReconciliationRequestedEvent.forProductSkus(
                9L, 7L, "product-1", List.of("sku-1", "sku-2")));

        verify(orderMapper).selectMutableReconciliationCandidateIdsByProductAndSkus(
                9L, 7L, "product-1", List.of("sku-1", "sku-2"), null, 500);
    }

    @Test
    void listenerRunsAfterCommitAsynchronouslyAndRestoresTenantContext() throws Exception {
        RentalChannelOrderReconciliationWorker worker =
                new RentalChannelOrderReconciliationWorker(
                        orderMapper, reconciliationService, runService);
        doAnswer(invocation -> {
            assertEquals(9L, TenantContextHolder.getRequiredTenantId());
            return List.of();
        }).when(orderMapper).selectMutableReconciliationCandidateIdsByItem(
                9L, 7L, "item-1", null, 500);
        TenantContextHolder.setTenantId(88L);

        worker.onRequested(RentalChannelOrderReconciliationRequestedEvent.forItem(
                9L, 7L, "item-1"));

        assertEquals(88L, TenantContextHolder.getRequiredTenantId());
        Method listener = RentalChannelOrderReconciliationWorker.class.getMethod(
                "onRequested", RentalChannelOrderReconciliationRequestedEvent.class);
        assertNotNull(listener.getAnnotation(Async.class));
        TransactionalEventListener annotation =
                listener.getAnnotation(TransactionalEventListener.class);
        assertNotNull(annotation);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
        assertTrue(annotation.fallbackExecution());
    }

    @Test
    void trackedRunPersistsAllOutcomeCounters() {
        RentalChannelOrderReconciliationWorker worker =
                new RentalChannelOrderReconciliationWorker(
                        orderMapper, reconciliationService, runService);
        when(orderMapper.selectMutableReconciliationCandidateIdsByItem(
                9L, 7L, "item-1", null, 500)).thenReturn(List.of(10L, 11L, 12L, 13L));
        when(reconciliationService.reconcile(10L))
                .thenReturn(RentalChannelOrderReconciliationResult.converted(
                        100L, RentalOrderPreparationDecision.ready(), true, "CREATED"));
        when(reconciliationService.reconcile(11L))
                .thenReturn(RentalChannelOrderReconciliationResult.skipped());
        when(reconciliationService.reconcile(12L))
                .thenReturn(RentalChannelOrderReconciliationResult.reviewRequired(
                        200L, "CONFLICT"));
        when(reconciliationService.reconcile(13L)).thenThrow(new IllegalStateException("bad row"));

        worker.onRequested(RentalChannelOrderReconciliationRequestedEvent.forTrackedItem(
                9L, 7L, "item-1", 80L));

        verify(runService).markRunning(80L);
        ArgumentCaptor<RentalChannelReconciliationCounters> captor =
                ArgumentCaptor.forClass(RentalChannelReconciliationCounters.class);
        verify(runService).complete(org.mockito.ArgumentMatchers.eq(80L), captor.capture());
        RentalChannelReconciliationCounters counters = captor.getValue();
        assertEquals(4, counters.scanned());
        assertEquals(1, counters.skipped());
        assertEquals(1, counters.created());
        assertEquals(1, counters.conflict());
        assertEquals(1, counters.failed());
        assertEquals(1, counters.reviewRequired());
    }

    @Test
    void trackedRunKeepsCompletedBatchCountersWhenLaterQueryFails() {
        RentalChannelOrderReconciliationWorker worker =
                new RentalChannelOrderReconciliationWorker(
                        orderMapper, reconciliationService, runService);
        List<Long> firstBatch = LongStream.rangeClosed(1, 500).boxed().toList();
        when(orderMapper.selectMutableReconciliationCandidateIdsByItem(
                9L, 7L, "item-1", null, 500)).thenReturn(firstBatch);
        when(orderMapper.selectMutableReconciliationCandidateIdsByItem(
                9L, 7L, "item-1", 500L, 500))
                .thenThrow(new IllegalStateException("database unavailable"));

        worker.onRequested(RentalChannelOrderReconciliationRequestedEvent.forTrackedItem(
                9L, 7L, "item-1", 81L));

        ArgumentCaptor<RentalChannelReconciliationCounters> captor =
                ArgumentCaptor.forClass(RentalChannelReconciliationCounters.class);
        verify(runService).fail(
                org.mockito.ArgumentMatchers.eq(81L),
                captor.capture(),
                org.mockito.ArgumentMatchers.eq("IllegalStateException"));
        assertEquals(500, captor.getValue().scanned());
        assertEquals(500, captor.getValue().unchanged());
    }

}
