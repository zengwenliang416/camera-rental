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
class XianyuProductWebhookPersistenceServiceTest {

    @Mock
    private XianyuPushEventMapper eventMapper;
    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuPushEventPublisher eventPublisher;

    private XianyuProductWebhookPersistenceService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new XianyuProductWebhookPersistenceService(eventMapper, rawPayloadMapper,
                new XianyuPayloadHasher(), new XianyuLogRedactor(objectMapper), eventPublisher,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void shouldStoreRedactedPayloadAndPublishDurableEvent() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        when(eventMapper.selectByTenantIdAndDedupeKeyForUpdate(eq(9L), any()))
                .thenReturn(XianyuPushEventDO.builder().id(41L).processingStatus("RECEIVED").build());
        XianyuProductPushPayload payload = payload();

        Long eventId = service.accept(9L, 77L, payload, rawBody(), service.dedupeKey(payload));

        assertEquals(41L, eventId);
        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        assertFalse(rawCaptor.getValue().getPayload().contains("private-user"));
        assertEquals("REDACTED_V1", rawCaptor.getValue().getRedactionVersion());
        ArgumentCaptor<XianyuPushEventDO> eventCaptor = ArgumentCaptor.forClass(XianyuPushEventDO.class);
        verify(eventMapper).insertOrReuse(eq(9L), eventCaptor.capture());
        assertEquals("PRODUCT_PUSH", eventCaptor.getValue().getEventType());
        verify(eventPublisher).publishAfterCommitOrNow(
                new XianyuProductPushReceivedEvent(9L, 41L, 77L, "441160510721413"));
    }

    @Test
    void shouldPersistUnmappedSellerWithoutPublishing() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        when(eventMapper.selectByTenantIdAndDedupeKeyForUpdate(eq(9L), any()))
                .thenReturn(XianyuPushEventDO.builder().id(41L).processingStatus("FAILED").build());

        service.accept(9L, null, payload(), rawBody(), "dedupe");

        ArgumentCaptor<XianyuPushEventDO> eventCaptor = ArgumentCaptor.forClass(XianyuPushEventDO.class);
        verify(eventMapper).insertOrReuse(eq(9L), eventCaptor.capture());
        assertEquals("FAILED", eventCaptor.getValue().getProcessingStatus());
        assertEquals("SHOP_MAPPING_UNAVAILABLE", eventCaptor.getValue().getLastErrorCode());
        verify(eventPublisher, never()).publishAfterCommitOrNow(any());
    }

    private XianyuProductPushPayload payload() {
        return new XianyuProductPushPayload("123456", "441160510721413", 22,
                3, 2, 5500L, 1, 1694000092L);
    }

    private String rawBody() {
        return """
                {"seller_id":123456,"product_id":441160510721413,"product_status":22,
                "publish_status":3,"item_biz_type":2,"price":5500,"stock":1,
                "user_name":"private-user","modify_time":1694000092}
                """;
    }

}
