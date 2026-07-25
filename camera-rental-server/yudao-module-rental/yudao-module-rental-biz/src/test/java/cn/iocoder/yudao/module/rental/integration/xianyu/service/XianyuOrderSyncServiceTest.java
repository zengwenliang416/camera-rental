package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncRunMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAlertAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuOrderSyncServiceTest {

    @Mock
    private XianyuReadClient readClient;
    @Mock
    private XianyuOrderPersistenceService persistenceService;
    @Mock
    private XianyuOrderMapper orderMapper;
    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuSyncRunMapper syncRunMapper;
    @Mock
    private XianyuAlertAdminService alertAdminService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private XianyuOrderSyncService service;
    private XianyuOrderSyncWindow window;

    @BeforeEach
    void setUp() {
        service = new XianyuOrderSyncService(readClient, new XianyuOrderListPageParser(), persistenceService,
                orderMapper, rawPayloadMapper, syncRunMapper, alertAdminService, new XianyuPayloadHasher(), objectMapper,
                Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC));
        window = new XianyuOrderSyncWindow(LocalDateTime.of(2026, 7, 22, 0, 0),
                LocalDateTime.of(2026, 7, 23, 0, 0), 1, 50);
        doAnswer(invocation -> {
            invocation.getArgument(0, XianyuSyncRunDO.class).setId(99L);
            return 1;
        }).when(syncRunMapper).insert(any(XianyuSyncRunDO.class));
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPersistEveryDetailWithoutAdvancingPageCursor() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response("""
                {"code":0,"data":{"count":2,"page_no":1,"page_size":50,"list":[
                {"order_no":"order-1","update_time":1784710800},
                {"order_no":"order-2","update_time":1784714400}]}}"""));
        when(readClient.execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any()))
                .thenReturn(response("{\"code\":0,\"data\":{\"order_no\":\"order-1\"}}"),
                        response("{\"code\":0,\"data\":{\"order_no\":\"order-2\"}}"));
        when(persistenceService.persistOrderDetail(eq(7L), any()))
                .thenReturn(order("order-1", LocalDateTime.of(2026, 7, 22, 23, 0)),
                        order("order-2", LocalDateTime.of(2026, 7, 23, 0, 0)));
        XianyuOrderPageSyncResult result = service.syncPage(7L, 88L, window);

        assertEquals(99L, result.syncRunId());
        assertEquals(2, result.receivedCount());
        assertEquals(2, result.succeededCount());
        assertFalse(result.cursorAdvanced());
        verify(persistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
        ArgumentCaptor<XianyuRawPayloadDO> payloadCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), payloadCaptor.capture());
        assertEquals(XianyuOrderSyncService.ORDER_PAGE_SOURCE_TYPE, payloadCaptor.getValue().getSourceType());
        assertEquals("order-page:7", payloadCaptor.getValue().getSourceIdentifier());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("SUCCEEDED", runCaptor.getValue().getStatus());
    }

    @Test
    void shouldReplayPersistedPageWithoutAdvancingCursor() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any()))
                .thenReturn(response("{\"code\":0,\"data\":{\"order_no\":\"order-1\"}}"));
        when(persistenceService.persistOrderDetail(eq(7L), any()))
                .thenReturn(order("order-1", LocalDateTime.of(2026, 7, 22, 23, 0)));

        XianyuOrderPageSyncResult result = service.replayPersistedPage(7L, """
                {"code":0,"data":{"count":1,"page_no":1,"page_size":50,"list":[
                {"order_no":"order-1","update_time":1784710800}]}}""");

        assertEquals(99L, result.syncRunId());
        assertEquals(1, result.receivedCount());
        assertEquals(1, result.succeededCount());
        assertFalse(result.cursorAdvanced());
        verify(rawPayloadMapper, never()).insertOrReuse(any(), any());
        verify(persistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals(XianyuOrderSyncService.TRIGGER_REPLAY, runCaptor.getValue().getTriggerType());
        assertEquals("SUCCEEDED", runCaptor.getValue().getStatus());
    }

    @Test
    void shouldFinishAnEmptyPageWithoutCursorMovement() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response("""
                {"code":0,"data":{"count":0,"page_no":1,"page_size":50,"list":[]}}"""));

        XianyuOrderPageSyncResult result = service.syncPage(7L, 88L, window);

        assertEquals(0, result.receivedCount());
        assertFalse(result.cursorAdvanced());
        verify(persistenceService, never()).persistOrderDetail(any(), any());
        verify(persistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
    }

    @Test
    void shouldNotAdvanceCursorWhenAnyDetailFails() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response("""
                {"code":0,"data":{"count":2,"page_no":1,"page_size":50,"list":[
                {"order_no":"order-1","update_time":1784710800},
                {"order_no":"order-2","update_time":1784714400}]}}"""));
        when(readClient.execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any()))
                .thenReturn(response("{\"code\":0,\"data\":{\"order_no\":\"order-1\"}}"))
                .thenThrow(new IllegalStateException("secret-bearing transport detail"));
        when(persistenceService.persistOrderDetail(eq(7L), any()))
                .thenReturn(order("order-1", LocalDateTime.of(2026, 7, 22, 23, 0)));

        assertThrows(IllegalStateException.class, () -> service.syncPage(7L, 88L, window));

        verify(persistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        XianyuSyncRunDO failedRun = runCaptor.getValue();
        assertEquals("FAILED", failedRun.getStatus());
        assertEquals(2, failedRun.getReceivedCount());
        assertEquals(1, failedRun.getSucceededCount());
        assertEquals("IllegalStateException", failedRun.getLastErrorCode());
        assertEquals("Order synchronization page failed", failedRun.getLastErrorMessage());
        verify(alertAdminService).recordSyncFailed(7L, "ORDER", "IllegalStateException");
    }

    @Test
    void shouldRejectTooLargeWindowBeforeDetailRefreshOrCursorAdvance() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response("""
                {"code":0,"data":{"count":10001,"page_no":1,"page_size":50,"list":[]}}"""));

        assertThrows(RuntimeException.class, () -> service.syncPage(7L, 88L, window));

        verify(readClient, never()).execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any());
        verify(persistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("WINDOW_TOO_LARGE", runCaptor.getValue().getLastErrorCode());
    }

    @Test
    void shouldRejectMismatchedResponsePageSize() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response("""
                {"code":0,"data":{"count":1,"page_no":1,"page_size":100,"list":[
                {"order_no":"order-1","update_time":1784710800}]}}"""));

        assertThrows(RuntimeException.class, () -> service.syncPage(7L, 88L, window));

        verify(readClient, never()).execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("PAGE_METADATA_MISMATCH", runCaptor.getValue().getLastErrorCode());
    }

    @Test
    void shouldRejectUnderfilledPageBeforeDetailRefresh() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response("""
                {"code":0,"data":{"count":2,"page_no":1,"page_size":50,"list":[
                {"order_no":"order-1","update_time":1784710800}]}}"""));

        assertThrows(RuntimeException.class, () -> service.syncPage(7L, 88L, window));

        verify(readClient, never()).execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("PAGE_METADATA_MISMATCH", runCaptor.getValue().getLastErrorCode());
    }

    @Test
    void shouldSkipDetailRefreshWhenStoredOrderIsAlreadyCurrent() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response("""
                {"code":0,"data":{"count":1,"page_no":1,"page_size":50,"list":[
                {"order_no":"order-1","update_time":1784710800}]}}"""));
        XianyuOrderDO current = order("order-1", LocalDateTime.of(2026, 7, 22, 17, 0));
        current.setRawPayloadId(31L);
        when(orderMapper.selectRefreshStateList(eq(7L), eq(List.of("order-1"))))
                .thenReturn(List.of(current));

        XianyuOrderPageSyncResult result = service.syncPage(7L, 88L, window);

        assertEquals(1, result.receivedCount());
        assertEquals(1, result.succeededCount());
        verify(readClient, never()).execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any());
        verify(persistenceService, never()).persistOrderDetail(any(), any());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals(1, runCaptor.getValue().getDeduplicatedCount());
    }

    @Test
    void shouldRejectWindowOutsideSixMonthRetentionBeforeRemoteCall() {
        XianyuOrderSyncWindow expired = new XianyuOrderSyncWindow(
                LocalDateTime.of(2025, 12, 1, 0, 0),
                LocalDateTime.of(2025, 12, 2, 0, 0), 1, 50);

        assertThrows(RuntimeException.class, () -> service.syncPage(7L, 88L, expired));

        verify(readClient, never()).execute(any(), any());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("WINDOW_OUTSIDE_RETENTION", runCaptor.getValue().getLastErrorCode());
    }

    private XianyuReadResponse response(String json) throws Exception {
        JsonNode payload = objectMapper.readTree(json);
        return new XianyuReadResponse(200, 0, payload, json);
    }

    private XianyuOrderDO order(String externalOrderId, LocalDateTime sourceUpdatedAt) {
        return XianyuOrderDO.builder().externalOrderId(externalOrderId).sourceUpdatedAt(sourceUpdatedAt).build();
    }

}
