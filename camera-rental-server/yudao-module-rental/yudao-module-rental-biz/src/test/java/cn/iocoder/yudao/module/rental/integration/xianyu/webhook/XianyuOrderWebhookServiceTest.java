package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuWebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuOrderWebhookServiceTest {

    @Mock
    private XianyuWebhookSignatureVerifier signatureVerifier;
    @Mock
    private XianyuOrderPushShopResolver shopResolver;
    @Mock
    private XianyuOrderWebhookPersistenceService persistenceService;

    private final XianyuOrderPushPayloadParser payloadParser =
            new XianyuOrderPushPayloadParser(new com.fasterxml.jackson.databind.ObjectMapper());
    private XianyuOrderWebhookService service;

    @BeforeEach
    void setUp() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setAppKey("test-app");
        properties.setAppSecret("test-secret");
        properties.setTenantId(9L);
        service = new XianyuOrderWebhookService(properties, signatureVerifier, payloadParser,
                shopResolver, persistenceService);
    }

    @Test
    void shouldRejectInvalidSignatureBeforeParsingOrPersistence() {
        when(signatureVerifier.verify(any(), anyLong(), any(), any())).thenReturn(false);

        XianyuWebhookReceipt receipt = service.receive("test-app", 1784890000L, "bad", validPayload());

        assertEquals("fail", receipt.result());
        verify(shopResolver, never()).resolveShopId(any(), any());
        verify(persistenceService, never()).accept(any(), any(), any(), any(), any());
    }

    @Test
    void shouldPersistWhenSellerMapsToExactlyOneValidShop() {
        when(signatureVerifier.verify(any(), anyLong(), any(), any())).thenReturn(true);
        when(shopResolver.resolveShopId("123456", "order-1")).thenReturn(77L);
        when(persistenceService.dedupeKey(any())).thenReturn("dedupe");

        XianyuWebhookReceipt receipt = service.receive("test-app", 1784890000L, "signature", validPayload());

        assertEquals("success", receipt.result());
        verify(persistenceService).accept(eq(9L), eq(77L), any(), eq(validPayload()), eq("dedupe"));
    }

    @Test
    void shouldDurablyAcceptWithoutGuessingAmbiguousSellerMapping() {
        when(signatureVerifier.verify(any(), anyLong(), any(), any())).thenReturn(true);
        when(shopResolver.resolveShopId("123456", "order-1")).thenReturn(null);
        when(persistenceService.dedupeKey(any())).thenReturn("dedupe");

        XianyuWebhookReceipt receipt = service.receive("test-app", 1784890000L, "signature", validPayload());

        assertEquals("success", receipt.result());
        verify(persistenceService).accept(eq(9L), eq(null), any(), eq(validPayload()), eq("dedupe"));
    }

    private String validPayload() {
        return """
                {"seller_id":123456,"user_name":"private-user","order_no":"order-1","order_type":1,
                "order_status":22,"refund_status":0,"modify_time":1784890000,
                "product_id":987654,"item_id":876543}
                """;
    }

}
