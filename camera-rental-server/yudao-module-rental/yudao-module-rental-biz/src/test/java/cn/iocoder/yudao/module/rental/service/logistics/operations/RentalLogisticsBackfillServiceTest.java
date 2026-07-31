package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import cn.iocoder.yudao.module.rental.service.logistics.WaybillPrivacy;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.BackfillCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RentalLogisticsBackfillServiceTest {

    private final RentalLogisticsOperationsMapper operationsMapper = mock(RentalLogisticsOperationsMapper.class);
    private final RentalLogisticsBackfillTransactionService transactionService =
            mock(RentalLogisticsBackfillTransactionService.class);
    private final WaybillPrivacy waybillPrivacy = mock(WaybillPrivacy.class);
    private final RentalLogisticsBackfillService service =
            new RentalLogisticsBackfillService(operationsMapper, transactionService, waybillPrivacy);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void defaultsToDryRunAndPerformsNoWritesOrProviderTaskEnqueue() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        when(operationsMapper.selectBackfillCandidates(9L, 20)).thenReturn(List.of(candidate));

        var result = service.backfill(new BackfillCommand(true, 20, true));

        assertTrue(result.dryRun());
        assertFalse(result.providerTasksEnqueued());
        assertEquals("PROVIDER_ENQUEUE_DEFERRED", result.providerTaskReason());
        assertEquals("ELIGIBLE", result.items().get(0).status());
        verifyNoInteractions(transactionService);
    }

    @Test
    void executionUsesLocalOnlyDeliveryAndIdempotentShipmentBinding() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        when(operationsMapper.selectBackfillCandidates(9L, 1)).thenReturn(List.of(candidate));
        when(transactionService.apply(candidate))
                .thenReturn(new RentalDeliveryResult(99L, true, "READY", "PROVIDER_DISABLED",
                        "PROVIDER_DISABLED", "SF5****2626", null, List.of()));

        var result = service.backfill(new BackfillCommand(false, 1, false));

        assertEquals(1, result.createdOrReusedCount());
        assertEquals(99L, result.items().get(0).deliveryId());
        verify(transactionService).apply(candidate);
    }

    @Test
    void incompleteHistoricalRowsAreReportedWithoutMutation() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        candidate.setRentalOrderItemId(null);
        when(operationsMapper.selectBackfillCandidates(9L, 10)).thenReturn(List.of(candidate));

        var result = service.backfill(new BackfillCommand(false, 10, false));

        assertEquals(1, result.skippedCount());
        assertEquals("BACKFILL_RELATION_INCOMPLETE", result.items().get(0).reason());
        verifyNoInteractions(transactionService);
    }

    @Test
    void missingWaybillNeverCallsMaskWithNull() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        candidate.setWaybillNo(null);
        when(operationsMapper.selectBackfillCandidates(9L, 10)).thenReturn(List.of(candidate));

        var result = service.backfill(new BackfillCommand(false, 10, false));

        assertEquals("BACKFILL_WAYBILL_MISSING", result.items().get(0).reason());
        assertNull(result.items().get(0).maskedWaybillNo());
        verify(waybillPrivacy, never()).mask(any());
        verifyNoInteractions(transactionService);
    }

    @Test
    void malformedWaybillIsNotEchoedAndDoesNotAbortBatch() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow incomplete = candidate();
        incomplete.setRentalOrderItemId(null);
        incomplete.setWaybillNo("bad/value");
        RentalLogisticsOperationsMapper.BackfillCandidateRow failed = candidate();
        failed.setShipmentId(11L);
        failed.setWaybillNo("bad/value");
        when(operationsMapper.selectBackfillCandidates(9L, 10)).thenReturn(List.of(incomplete, failed));
        when(waybillPrivacy.mask("bad/value"))
                .thenThrow(new RentalLogisticsException("WAYBILL_INVALID"));
        when(transactionService.apply(failed))
                .thenThrow(new RentalLogisticsException("WAYBILL_INVALID"));

        var result = service.backfill(new BackfillCommand(false, 10, false));

        assertEquals(2, result.skippedCount());
        assertEquals(2, result.items().size());
        assertTrue(result.items().stream().allMatch(item -> item.maskedWaybillNo() == null));
        assertTrue(result.items().stream().noneMatch(item -> item.toString().contains("bad/value")));
    }

    private RentalLogisticsOperationsMapper.BackfillCandidateRow candidate() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow row =
                new RentalLogisticsOperationsMapper.BackfillCandidateRow();
        row.setShipmentId(10L);
        row.setChannelOrderId(20L);
        row.setAssignmentId(30L);
        row.setDeviceId(40L);
        row.setRentalOrderId(500L);
        row.setRentalOrderItemId(600L);
        row.setReceiverMobile("13800138000");
        row.setWaybillNo("SF5159187992626");
        row.setExpressCode("SF");
        row.setExpressName("顺丰速运");
        return row;
    }
}
