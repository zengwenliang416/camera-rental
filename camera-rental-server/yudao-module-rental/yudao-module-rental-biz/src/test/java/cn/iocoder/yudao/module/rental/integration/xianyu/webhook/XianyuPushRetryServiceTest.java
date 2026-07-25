package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuPushRetryServiceTest {

    @Mock
    private XianyuPushEventMapper eventMapper;
    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuPushEventStateService stateService;
    @Mock
    private XianyuPushEventPublisher eventPublisher;
    @Mock
    private XianyuOrderPushShopResolver shopResolver;
    @Mock
    private XianyuProductPushShopResolver productShopResolver;

    @Test
    void shouldQueueOnlyDurableStaleCandidate() {
        XianyuProperties properties = new XianyuProperties();
        properties.setTenantId(9L);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(41L)
                .rawPayloadId(31L)
                .processingStatus("FAILED")
                .build();
        when(eventMapper.selectRetryCandidates(any(LocalDateTime.class), eq(100))).thenReturn(List.of(event));
        when(stateService.prepareRetry(eq(41L), any(LocalDateTime.class))).thenReturn(true);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 31L)).thenReturn(XianyuRawPayloadDO.builder()
                .payload(validStoredPayload())
                .build());
        when(shopResolver.resolveShopId("123456", "order-1")).thenReturn(77L);
        XianyuPushRetryService service = new XianyuPushRetryService(
                properties, eventMapper, rawPayloadMapper,
                new XianyuOrderPushPayloadParser(new ObjectMapper()),
                new XianyuProductPushPayloadParser(new ObjectMapper()),
                shopResolver, productShopResolver, stateService, eventPublisher,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));

        assertEquals("candidates=1 queued=1 failed=0", service.retryStaleEvents());
        verify(eventPublisher).publishAfterCommitOrNow(
                new XianyuOrderPushReceivedEvent(9L, 41L, 77L, "order-1"));
    }

    @Test
    void shouldManuallyReplayFailedPushEventOnce() {
        XianyuProperties properties = new XianyuProperties();
        properties.setTenantId(9L);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(42L)
                .rawPayloadId(32L)
                .processingStatus("FAILED")
                .build();
        when(eventMapper.selectByTenantIdAndId(9L, 42L)).thenReturn(event);
        when(stateService.prepareManualReplay(42L, "7")).thenReturn(true);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 32L)).thenReturn(XianyuRawPayloadDO.builder()
                .payload(validStoredPayload())
                .build());
        when(shopResolver.resolveShopId("123456", "order-1")).thenReturn(77L);
        XianyuPushRetryService service = service(properties);

        XianyuPushReplayOutcome outcome = service.replayPushEvent(42L, 7L);

        assertEquals("QUEUED", outcome.status());
        verify(eventPublisher).publishAfterCommitOrNow(
                new XianyuOrderPushReceivedEvent(9L, 42L, 77L, "order-1"));
    }

    @Test
    void shouldManuallyReplayProductPushEventOnce() {
        XianyuProperties properties = new XianyuProperties();
        properties.setTenantId(9L);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(45L)
                .eventType(XianyuProductWebhookPersistenceService.EVENT_TYPE)
                .rawPayloadId(35L)
                .processingStatus("FAILED")
                .build();
        when(eventMapper.selectByTenantIdAndId(9L, 45L)).thenReturn(event);
        when(stateService.prepareManualReplay(45L, "7")).thenReturn(true);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 35L)).thenReturn(XianyuRawPayloadDO.builder()
                .payload(validStoredProductPayload())
                .build());
        when(productShopResolver.resolveShopId("123456", "441160510721413")).thenReturn(88L);
        XianyuPushRetryService service = service(properties);

        XianyuPushReplayOutcome outcome = service.replayPushEvent(45L, 7L);

        assertEquals("QUEUED", outcome.status());
        verify(eventPublisher).publishAfterCommitOrNow(
                new XianyuProductPushReceivedEvent(9L, 45L, 88L, "441160510721413"));
    }

    @Test
    void shouldReplayProductPushFromExternalIdentifierWhenStoredPayloadIsRedacted() {
        XianyuProperties properties = new XianyuProperties();
        properties.setTenantId(9L);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(46L)
                .eventType(XianyuProductWebhookPersistenceService.EVENT_TYPE)
                .externalIdentifier("441160510721413")
                .rawPayloadId(36L)
                .processingStatus("FAILED")
                .build();
        when(eventMapper.selectByTenantIdAndId(9L, 46L)).thenReturn(event);
        when(stateService.prepareManualReplay(46L, "7")).thenReturn(true);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 36L)).thenReturn(XianyuRawPayloadDO.builder()
                .payload("{\"seller_id\":\"***\",\"product_id\":\"***\"}")
                .build());
        when(productShopResolver.resolveShopId(null, "441160510721413")).thenReturn(88L);
        XianyuPushRetryService service = service(properties);

        XianyuPushReplayOutcome outcome = service.replayPushEvent(46L, 7L);

        assertEquals("QUEUED", outcome.status());
        verify(eventPublisher).publishAfterCommitOrNow(
                new XianyuProductPushReceivedEvent(9L, 46L, 88L, "441160510721413"));
    }

    @Test
    void shouldSkipManualReplayWhenEventCannotBePrepared() {
        XianyuProperties properties = new XianyuProperties();
        properties.setTenantId(9L);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(43L)
                .processingStatus("SUCCEEDED")
                .build();
        when(eventMapper.selectByTenantIdAndId(9L, 43L)).thenReturn(event);
        when(stateService.prepareManualReplay(43L, "7")).thenReturn(false);
        XianyuPushRetryService service = service(properties);

        XianyuPushReplayOutcome outcome = service.replayPushEvent(43L, 7L);

        assertEquals("SKIPPED", outcome.status());
        verify(eventPublisher, never()).publishAfterCommitOrNow(any());
    }

    @Test
    void shouldMarkManualReplayPreparationFailureWithSafeErrorCode() {
        XianyuProperties properties = new XianyuProperties();
        properties.setTenantId(9L);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(44L)
                .rawPayloadId(34L)
                .processingStatus("FAILED")
                .build();
        when(eventMapper.selectByTenantIdAndId(9L, 44L)).thenReturn(event);
        when(stateService.prepareManualReplay(44L, "7")).thenReturn(true);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 34L)).thenReturn(null);
        XianyuPushRetryService service = service(properties);

        XianyuPushReplayOutcome outcome = service.replayPushEvent(44L, 7L);

        assertEquals("FAILED", outcome.status());
        verify(stateService).markRetryPreparationFailed(eq(44L), anyString());
        verify(eventPublisher, never()).publishAfterCommitOrNow(any());
    }

    private XianyuPushRetryService service(XianyuProperties properties) {
        return new XianyuPushRetryService(
                properties, eventMapper, rawPayloadMapper,
                new XianyuOrderPushPayloadParser(new ObjectMapper()),
                new XianyuProductPushPayloadParser(new ObjectMapper()),
                shopResolver, productShopResolver, stateService, eventPublisher,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));
    }

    private String validStoredPayload() {
        return """
                {"seller_id":123456,"user_name":"***","order_no":"order-1","order_type":1,
                "order_status":22,"refund_status":0,"modify_time":1784890000,
                "product_id":987654,"item_id":876543}
                """;
    }

    private String validStoredProductPayload() {
        return """
                {"seller_id":123456,"product_id":441160510721413,"product_status":22,
                "publish_status":3,"item_biz_type":2,"price":5500,"stock":1,
                "user_name":"***","modify_time":1694000092}
                """;
    }

}
