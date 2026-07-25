package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPushReplayRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadReplayRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushReplayOutcome;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushRetryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_PUSH_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_RAW_PAYLOAD_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_RAW_PAYLOAD_REPLAY_UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuReplayAdminServiceTest {

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void replayPushEventShouldRejectMissingEvent() {
        XianyuPushEventMapper eventMapper = mock(XianyuPushEventMapper.class);
        XianyuPushRetryService retryService = mock(XianyuPushRetryService.class);
        XianyuReplayAdminService service = service(eventMapper, mock(XianyuRawPayloadMapper.class),
                retryService, mock(XianyuOrderPersistenceService.class),
                mock(XianyuOrderSyncService.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.replayPushEvent(1L, 9L));

        assertEquals(XIANYU_PUSH_EVENT_NOT_EXISTS.getCode(), exception.getCode());
        verify(eventMapper).selectByTenantIdAndId(9L, 1L);
        verify(retryService, never()).replayPushEvent(any(), any());
    }

    @Test
    void replayPushEventShouldReturnMaskedOutcome() {
        XianyuPushEventMapper eventMapper = mock(XianyuPushEventMapper.class);
        XianyuPushRetryService retryService = mock(XianyuPushRetryService.class);
        when(eventMapper.selectByTenantIdAndId(9L, 2L)).thenReturn(XianyuPushEventDO.builder().id(2L).build());
        when(retryService.replayPushEvent(2L, 9L)).thenReturn(new XianyuPushReplayOutcome(
                2L, "FAILED", "REMOTE_ERROR", "联系人:张三 手机13800138000"));
        XianyuReplayAdminService service = service(eventMapper, mock(XianyuRawPayloadMapper.class),
                retryService, mock(XianyuOrderPersistenceService.class), mock(XianyuOrderSyncService.class));

        XianyuPushReplayRespVO vo = service.replayPushEvent(2L, 9L);

        assertEquals("FAILED", vo.getStatus());
        assertFalse(vo.getMessage().contains("张三"));
        assertFalse(vo.getMessage().contains("13800138000"));
        verify(eventMapper).selectByTenantIdAndId(9L, 2L);
    }

    @Test
    void replayRawPayloadShouldRejectMissingPayload() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        XianyuReplayAdminService service = service(mock(XianyuPushEventMapper.class), rawPayloadMapper,
                mock(XianyuPushRetryService.class), mock(XianyuOrderPersistenceService.class),
                mock(XianyuOrderSyncService.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.replayRawPayload(3L, 9L));

        assertEquals(XIANYU_RAW_PAYLOAD_NOT_EXISTS.getCode(), exception.getCode());
        verify(rawPayloadMapper).selectByTenantIdAndId(9L, 3L);
    }

    @Test
    void replayRawPayloadShouldRejectUnsupportedSourceType() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 4L)).thenReturn(XianyuRawPayloadDO.builder()
                .id(4L)
                .sourceType("AFTER_SALE_LIST_ITEM")
                .sourceIdentifier("after-sale:1")
                .build());
        XianyuReplayAdminService service = service(mock(XianyuPushEventMapper.class), rawPayloadMapper,
                mock(XianyuPushRetryService.class), mock(XianyuOrderPersistenceService.class),
                mock(XianyuOrderSyncService.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.replayRawPayload(4L, 9L));

        assertEquals(XIANYU_RAW_PAYLOAD_REPLAY_UNSUPPORTED.getCode(), exception.getCode());
    }

    @Test
    void replayRawPayloadShouldPersistOrderDetailWithoutAdvancingCursor() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        XianyuOrderPersistenceService orderPersistenceService = mock(XianyuOrderPersistenceService.class);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 5L)).thenReturn(XianyuRawPayloadDO.builder()
                .id(5L)
                .sourceType("ORDER_DETAIL")
                .sourceIdentifier("order:88:order-1")
                .payload("{\"code\":0,\"data\":{\"order_no\":\"order-1\"}}")
                .build());
        when(orderPersistenceService.persistOrderDetail(88L, "{\"code\":0,\"data\":{\"order_no\":\"order-1\"}}"))
                .thenReturn(XianyuOrderDO.builder().id(66L).build());
        XianyuReplayAdminService service = service(mock(XianyuPushEventMapper.class), rawPayloadMapper,
                mock(XianyuPushRetryService.class), orderPersistenceService, mock(XianyuOrderSyncService.class));

        XianyuRawPayloadReplayRespVO vo = service.replayRawPayload(5L, 9L);

        assertEquals(5L, vo.getRawPayloadId());
        assertEquals(66L, vo.getOrderId());
        assertEquals("REPLAYED", vo.getStatus());
        verify(orderPersistenceService).persistOrderDetail(88L, "{\"code\":0,\"data\":{\"order_no\":\"order-1\"}}");
        verify(orderPersistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
    }

    @Test
    void replayRawPayloadShouldReturnSafeFailureWhenPersistenceFails() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        XianyuOrderPersistenceService orderPersistenceService = mock(XianyuOrderPersistenceService.class);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 6L)).thenReturn(XianyuRawPayloadDO.builder()
                .id(6L)
                .sourceType("ORDER_DETAIL")
                .sourceIdentifier("order:88:order-2")
                .payload("not-json")
                .build());
        when(orderPersistenceService.persistOrderDetail(88L, "not-json"))
                .thenThrow(new IllegalStateException("customer phone 13800138000"));
        XianyuReplayAdminService service = service(mock(XianyuPushEventMapper.class), rawPayloadMapper,
                mock(XianyuPushRetryService.class), orderPersistenceService, mock(XianyuOrderSyncService.class));

        XianyuRawPayloadReplayRespVO vo = service.replayRawPayload(6L, 9L);

        assertEquals("FAILED", vo.getStatus());
        assertEquals("IllegalStateException", vo.getSafeErrorCode());
        assertFalse(vo.getMessage().contains("13800138000"));
        verify(orderPersistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
    }

    @Test
    void replayRawPayloadShouldReplayOrderPageWithoutAdvancingCursor() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        XianyuOrderPersistenceService orderPersistenceService = mock(XianyuOrderPersistenceService.class);
        XianyuOrderSyncService orderSyncService = mock(XianyuOrderSyncService.class);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 7L)).thenReturn(XianyuRawPayloadDO.builder()
                .id(7L)
                .sourceType("ORDER_PAGE")
                .sourceIdentifier("order-page:88")
                .payload("{\"code\":0,\"data\":{\"count\":1,\"list\":[]}}")
                .build());
        when(orderSyncService.replayPersistedPage(88L, "{\"code\":0,\"data\":{\"count\":1,\"list\":[]}}"))
                .thenReturn(new XianyuOrderPageSyncResult(99L, 1, 1, false));
        XianyuReplayAdminService service = service(mock(XianyuPushEventMapper.class), rawPayloadMapper,
                mock(XianyuPushRetryService.class), orderPersistenceService, orderSyncService);

        XianyuRawPayloadReplayRespVO vo = service.replayRawPayload(7L, 9L);

        assertEquals("REPLAYED", vo.getStatus());
        assertEquals(7L, vo.getRawPayloadId());
        verify(orderSyncService).replayPersistedPage(88L, "{\"code\":0,\"data\":{\"count\":1,\"list\":[]}}");
        verify(orderPersistenceService, never()).persistOrderDetail(any(), any());
        verify(orderPersistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
    }

    @Test
    void replayRawPayloadShouldReturnSafeFailureWhenOrderPageReplayFails() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        XianyuOrderSyncService orderSyncService = mock(XianyuOrderSyncService.class);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 8L)).thenReturn(XianyuRawPayloadDO.builder()
                .id(8L)
                .sourceType("ORDER_PAGE")
                .sourceIdentifier("order-page:88")
                .payload("not-json")
                .build());
        when(orderSyncService.replayPersistedPage(88L, "not-json"))
                .thenThrow(new IllegalStateException("customer phone 13800138000"));
        XianyuReplayAdminService service = service(mock(XianyuPushEventMapper.class), rawPayloadMapper,
                mock(XianyuPushRetryService.class), mock(XianyuOrderPersistenceService.class), orderSyncService);

        XianyuRawPayloadReplayRespVO vo = service.replayRawPayload(8L, 9L);

        assertEquals("FAILED", vo.getStatus());
        assertEquals("IllegalStateException", vo.getSafeErrorCode());
        assertFalse(vo.getMessage().contains("13800138000"));
    }

    private XianyuReplayAdminService service(XianyuPushEventMapper eventMapper,
                                             XianyuRawPayloadMapper rawPayloadMapper,
                                             XianyuPushRetryService retryService,
                                             XianyuOrderPersistenceService orderPersistenceService,
                                             XianyuOrderSyncService orderSyncService) {
        return new XianyuReplayAdminService(eventMapper, rawPayloadMapper, retryService, orderPersistenceService,
                orderSyncService);
    }

}
