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

import java.time.LocalDate;
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
        when(operationsMapper.selectBackfillCandidates(9L, null, null, 20)).thenReturn(List.of(candidate));
        when(operationsMapper.selectChannelOrderBackfillCandidates(9L, null, null, 19)).thenReturn(List.of());

        var result = service.backfill(new BackfillCommand(true, 20, true));

        assertTrue(result.dryRun());
        assertFalse(result.providerTasksEnqueued());
        assertEquals("PROVIDER_ENQUEUE_DISABLED", result.providerTaskReason());
        assertEquals("ELIGIBLE", result.items().get(0).status());
        verifyNoInteractions(transactionService);
    }

    @Test
    void executionUsesLocalOnlyDeliveryAndIdempotentShipmentBinding() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        when(operationsMapper.selectBackfillCandidates(9L, null, null, 1)).thenReturn(List.of(candidate));
        when(transactionService.apply(candidate, false))
                .thenReturn(new RentalDeliveryResult(99L, true, "READY", "PROVIDER_DISABLED",
                        "PROVIDER_DISABLED", "SF5****2626", null, List.of()));

        var result = service.backfill(new BackfillCommand(false, 1, false));

        assertEquals(1, result.createdOrReusedCount());
        assertEquals(99L, result.items().get(0).deliveryId());
        verify(transactionService).apply(candidate, false);
    }

    @Test
    void incompleteHistoricalRowsAreReportedWithoutMutation() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        candidate.setRentalOrderItemId(null);
        when(operationsMapper.selectBackfillCandidates(9L, null, null, 10)).thenReturn(List.of(candidate));
        when(operationsMapper.selectChannelOrderBackfillCandidates(9L, null, null, 9)).thenReturn(List.of());

        var result = service.backfill(new BackfillCommand(false, 10, false));

        assertEquals(1, result.skippedCount());
        assertEquals("BACKFILL_RELATION_INCOMPLETE", result.items().get(0).reason());
        verifyNoInteractions(transactionService);
    }

    @Test
    void missingWaybillNeverCallsMaskWithNull() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        candidate.setWaybillNo(null);
        when(operationsMapper.selectBackfillCandidates(9L, null, null, 10)).thenReturn(List.of(candidate));
        when(operationsMapper.selectChannelOrderBackfillCandidates(9L, null, null, 9)).thenReturn(List.of());

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
        when(operationsMapper.selectBackfillCandidates(9L, null, null, 10))
                .thenReturn(List.of(incomplete, failed));
        when(operationsMapper.selectChannelOrderBackfillCandidates(9L, null, null, 8)).thenReturn(List.of());
        when(waybillPrivacy.mask("bad/value"))
                .thenThrow(new RentalLogisticsException("WAYBILL_INVALID"));
        when(transactionService.apply(failed, false))
                .thenThrow(new RentalLogisticsException("WAYBILL_INVALID"));

        var result = service.backfill(new BackfillCommand(false, 10, false));

        assertEquals(2, result.skippedCount());
        assertEquals(2, result.items().size());
        assertTrue(result.items().stream().allMatch(item -> item.maskedWaybillNo() == null));
        assertTrue(result.items().stream().noneMatch(item -> item.toString().contains("bad/value")));
    }

    @Test
    void scopesHistoricalCandidatesByInclusiveConsignDateRange() {
        LocalDate start = LocalDate.of(2026, 7, 30);
        LocalDate end = LocalDate.of(2026, 7, 31);
        when(operationsMapper.selectBackfillCandidates(9L, start, end, 70)).thenReturn(List.of());
        when(operationsMapper.selectChannelOrderBackfillCandidates(9L, start, end, 70))
                .thenReturn(List.of());

        service.backfill(new BackfillCommand(true, 70, false, start, end));

        verify(operationsMapper).selectBackfillCandidates(9L, start, end, 70);
        verify(operationsMapper).selectChannelOrderBackfillCandidates(9L, start, end, 70);
    }

    @Test
    void countsDistinctWaybillsWithoutExposingFullNumbers() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow first = candidate();
        RentalLogisticsOperationsMapper.BackfillCandidateRow duplicate = candidate();
        duplicate.setShipmentId(11L);
        RentalLogisticsOperationsMapper.BackfillCandidateRow second = candidate();
        second.setShipmentId(12L);
        second.setWaybillNo("SF5159187999999");
        when(operationsMapper.selectBackfillCandidates(9L, null, null, 10))
                .thenReturn(List.of(first, duplicate, second));
        when(operationsMapper.selectChannelOrderBackfillCandidates(9L, null, null, 7))
                .thenReturn(List.of());
        when(waybillPrivacy.mask(anyString())).thenReturn("SF5****9999");

        var result = service.backfill(new BackfillCommand(true, 10, false));

        assertEquals(2, result.distinctWaybillCount());
        assertTrue(result.items().stream().noneMatch(item -> item.toString().contains("SF5159187992626")));
    }

    @Test
    void rejectsReversedConsignDateRange() {
        BackfillCommand command = new BackfillCommand(true, 20, false,
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 30));

        RentalLogisticsException exception =
                assertThrows(RentalLogisticsException.class, () -> service.backfill(command));

        assertEquals("BACKFILL_DATE_RANGE_INVALID", exception.getCode());
        verifyNoInteractions(operationsMapper, transactionService);
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
