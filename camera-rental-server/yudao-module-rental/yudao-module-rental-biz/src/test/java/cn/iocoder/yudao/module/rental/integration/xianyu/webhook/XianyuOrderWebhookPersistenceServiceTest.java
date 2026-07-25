package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuLogRedactor;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuPayloadHasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuOrderWebhookPersistenceServiceTest {

    @Mock
    private XianyuPushEventMapper eventMapper;
    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuPushEventPublisher eventPublisher;

    private XianyuOrderWebhookPersistenceService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new XianyuOrderWebhookPersistenceService(eventMapper, rawPayloadMapper,
                new XianyuPayloadHasher(), new XianyuLogRedactor(objectMapper), eventPublisher,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void shouldStoreRedactedPayloadAndPublishDurableEvent() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any())).thenReturn(
                XianyuRawPayloadDO.builder().id(31L).build());
        when(eventMapper.selectByTenantIdAndDedupeKeyForUpdate(eq(9L), any())).thenReturn(
                XianyuPushEventDO.builder().id(41L).processingStatus("RECEIVED").build());
        XianyuOrderPushPayload payload = payload();
        String rawBody = rawBody();

        Long eventId = service.accept(9L, 77L, payload, rawBody, service.dedupeKey(payload));

        assertEquals(41L, eventId);
        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        assertFalse(rawCaptor.getValue().getPayload().contains("private-user"));
        assertEquals("REDACTED_V1", rawCaptor.getValue().getRedactionVersion());
        verify(eventPublisher).publishAfterCommitOrNow(new XianyuOrderPushReceivedEvent(9L, 41L, 77L, "order-1"));
    }

    @Test
    void shouldReuseSuccessfulDuplicateWithoutPublishingAgain() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any())).thenReturn(
                XianyuRawPayloadDO.builder().id(31L).build());
        when(eventMapper.selectByTenantIdAndDedupeKeyForUpdate(9L, "dedupe")).thenReturn(
                XianyuPushEventDO.builder().id(41L).processingStatus("SUCCEEDED").build());

        Long eventId = service.accept(9L, 77L, payload(), rawBody(), "dedupe");

        assertEquals(41L, eventId);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), any(XianyuRawPayloadDO.class));
        verify(eventPublisher, never()).publishAfterCommitOrNow(any());
    }

    @Test
    void shouldRepublishReceivedDuplicateForIdempotentConsumerClaim() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any())).thenReturn(
                XianyuRawPayloadDO.builder().id(31L).build());
        when(eventMapper.selectByTenantIdAndDedupeKeyForUpdate(9L, "dedupe")).thenReturn(
                XianyuPushEventDO.builder().id(41L).processingStatus("RECEIVED").build());

        Long eventId = service.accept(9L, 77L, payload(), rawBody(), "dedupe");

        assertEquals(41L, eventId);
        verify(eventPublisher).publishAfterCommitOrNow(new XianyuOrderPushReceivedEvent(9L, 41L, 77L, "order-1"));
    }

    @Test
    void shouldPersistUnmappedSellerForLaterRecoveryWithoutPublishing() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any())).thenReturn(
                XianyuRawPayloadDO.builder().id(31L).build());
        when(eventMapper.selectByTenantIdAndDedupeKeyForUpdate(eq(9L), any())).thenReturn(
                XianyuPushEventDO.builder().id(41L).processingStatus("FAILED").build());

        service.accept(9L, null, payload(), rawBody(), "dedupe");

        ArgumentCaptor<XianyuPushEventDO> eventCaptor = ArgumentCaptor.forClass(XianyuPushEventDO.class);
        verify(eventMapper).insertOrReuse(eq(9L), eventCaptor.capture());
        assertEquals("FAILED", eventCaptor.getValue().getProcessingStatus());
        assertEquals("SHOP_MAPPING_UNAVAILABLE", eventCaptor.getValue().getLastErrorCode());
        verify(eventPublisher, never()).publishAfterCommitOrNow(any());
    }

    @Test
    void shouldAtomicallyRecoverDuplicateFailedEventAfterShopMappingAppears() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any())).thenReturn(
                XianyuRawPayloadDO.builder().id(31L).build());
        XianyuPushEventDO existing = XianyuPushEventDO.builder()
                .id(41L)
                .processingStatus("FAILED")
                .lastErrorCode("SHOP_MAPPING_UNAVAILABLE")
                .lastErrorMessage("missing")
                .build();
        when(eventMapper.selectByTenantIdAndDedupeKeyForUpdate(9L, "dedupe")).thenReturn(existing);

        Long eventId = service.accept(9L, 77L, payload(), rawBody(), "dedupe");

        assertEquals(41L, eventId);
        assertEquals("RECEIVED", existing.getProcessingStatus());
        verify(eventMapper).updateById(existing);
        verify(eventPublisher).publishAfterCommitOrNow(new XianyuOrderPushReceivedEvent(9L, 41L, 77L, "order-1"));
    }

    private XianyuOrderPushPayload payload() {
        return new XianyuOrderPushPayload("123456", "order-1", 1, 22, 0,
                1784890000L, 987654L, 876543L);
    }

    private String rawBody() {
        return """
                {"seller_id":123456,"user_name":"private-user","order_no":"order-1","order_type":1,
                "order_status":22,"refund_status":0,"modify_time":1784890000,
                "product_id":987654,"item_id":876543}
                """;
    }

}
