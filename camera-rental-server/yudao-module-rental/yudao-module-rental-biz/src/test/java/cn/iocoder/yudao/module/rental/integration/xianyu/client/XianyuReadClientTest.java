package cn.iocoder.yudao.module.rental.integration.xianyu.client;

import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuLogRedactor;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuWebhookSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuReadClientTest {

    private static final long FIXED_TIMESTAMP = 1_700_000_000L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XianyuRequestSigner requestSigner = new XianyuRequestSigner();
    private MockWebServer mockWebServer;
    private XianyuProperties properties;
    private XianyuRuntimeConfigService runtimeConfigService;
    private XianyuReadClient client;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        properties = readyProperties();
        properties.setBaseUrl(mockWebServer.url("/").toString());
        runtimeConfigService = mock(XianyuRuntimeConfigService.class);
        when(runtimeConfigService.getCurrent()).thenReturn(properties);
        when(runtimeConfigService.findByAppKey("demo-app")).thenReturn(properties);
        client = new XianyuReadClient(runtimeConfigService, new XianyuCanonicalJson(objectMapper), requestSigner,
                new OkHttpClient(), objectMapper, Clock.fixed(Instant.ofEpochSecond(FIXED_TIMESTAMP), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void shouldSignAndSendTheSameCanonicalUtf8Body() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{\"code\":0,\"msg\":\"OK\",\"data\":{\"list\":[]}}")
                .addHeader("Content-Type", "application/json"));
        ObjectNode requestBody = objectMapper.createObjectNode()
                .put("z", "相机")
                .put("a", "");

        client.execute(XianyuReadEndpoint.ORDERS, requestBody);

        RecordedRequest request = mockWebServer.takeRequest();
        String expectedBody = "{\"a\":\"\",\"z\":\"相机\"}";
        assertEquals(expectedBody, request.getBody().readUtf8());
        assertEquals("/api/open/order/list", request.getRequestUrl().encodedPath());
        assertEquals("demo-app", request.getRequestUrl().queryParameter("appid"));
        assertEquals(Long.toString(FIXED_TIMESTAMP), request.getRequestUrl().queryParameter("timestamp"));
        assertEquals(requestSigner.sign("demo-app", "demo-secret", FIXED_TIMESTAMP, expectedBody),
                request.getRequestUrl().queryParameter("sign"));
    }

    @Test
    void shouldSendEmptyObjectForBodylessReadEndpoint() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{\"code\":0,\"msg\":\"OK\",\"data\":{\"list\":[]}}"));

        client.execute(XianyuReadEndpoint.AUTHORIZED_SHOPS, null);

        assertEquals("{}", mockWebServer.takeRequest().getBody().readUtf8());
    }

    @Test
    void shouldSendOptionalSellerIdQueryParameterForProductReads() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{\"code\":0,\"msg\":\"OK\",\"data\":{\"list\":[]}}"));

        client.execute(XianyuReadEndpoint.PRODUCTS, objectMapper.createObjectNode(), "123456");

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/api/open/product/list", request.getRequestUrl().encodedPath());
        assertEquals("123456", request.getRequestUrl().queryParameter("seller_id"));
    }

    @Test
    void shouldRejectDisabledIntegrationBeforeNetworkCall() {
        properties.setEnabled(false);

        XianyuClientException exception = assertThrows(XianyuClientException.class,
                () -> client.execute(XianyuReadEndpoint.ORDERS, objectMapper.createObjectNode()));

        assertEquals(XianyuClientException.Kind.INTEGRATION_DISABLED, exception.getKind());
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    void shouldClassifyRemoteErrorWithoutExposingMessage() {
        mockWebServer.enqueue(new MockResponse().setBody("{\"code\":500,\"msg\":\"receiver_mobile=13800138000\"}"));

        XianyuClientException exception = assertThrows(XianyuClientException.class,
                () -> client.execute(XianyuReadEndpoint.ORDER_DETAIL, objectMapper.createObjectNode().put("order_no", "123")));

        assertEquals(XianyuClientException.Kind.REMOTE_RESPONSE, exception.getKind());
        assertEquals(500, exception.getRemoteCode());
        assertFalse(exception.getMessage().contains("13800138000"));
    }

    @Test
    void shouldRejectResponseWithoutNumericCode() {
        mockWebServer.enqueue(new MockResponse().setBody("{\"msg\":\"OK\"}"));

        XianyuClientException exception = assertThrows(XianyuClientException.class,
                () -> client.execute(XianyuReadEndpoint.EXPRESS_COMPANIES, null));

        assertEquals(XianyuClientException.Kind.MALFORMED_RESPONSE, exception.getKind());
    }

    @Test
    void shouldKeepOutboundEndpointAllowlistReadOnly() {
        assertEquals(11, XianyuReadEndpoint.values().length);
        assertTrue(Arrays.stream(XianyuReadEndpoint.values())
                .map(XianyuReadEndpoint::getPath)
                .noneMatch(path -> path.contains("create") || path.contains("edit") || path.contains("delete")
                        || path.contains("consign") || path.contains("change-price") || path.contains("agree")
                        || path.contains("refuse")));
    }

    @Test
    void shouldVerifyRawWebhookBodyWithinTimestampWindow() {
        properties.setTenantId(42L);
        String rawBody = "{\"order_no\":\"123\",\"order_status\":12}";
        String signature = requestSigner.sign("demo-app", "demo-secret", FIXED_TIMESTAMP, rawBody);
        XianyuWebhookSignatureVerifier verifier = new XianyuWebhookSignatureVerifier(runtimeConfigService, requestSigner,
                Clock.fixed(Instant.ofEpochSecond(FIXED_TIMESTAMP), ZoneOffset.UTC));

        assertTrue(verifier.verify("demo-app", FIXED_TIMESTAMP, rawBody, signature));
        XianyuProperties verified = verifier.resolveVerifiedConfig(
                "demo-app", FIXED_TIMESTAMP, rawBody, signature);
        assertNotNull(verified);
        assertEquals(42L, verified.requireTenantId());
        verify(runtimeConfigService, org.mockito.Mockito.atLeastOnce()).findByAppKey("demo-app");
        assertFalse(verifier.verify("demo-app", FIXED_TIMESTAMP, rawBody + " ", signature));
        assertFalse(verifier.verify("demo-app", FIXED_TIMESTAMP - 301, rawBody, signature));
        assertFalse(verifier.verify("demo-app", FIXED_TIMESTAMP, null, signature));
    }

    @Test
    void shouldResolvePersistedConfigForEveryRequest() {
        XianyuProperties disabled = new XianyuProperties();
        when(runtimeConfigService.getCurrent()).thenReturn(disabled, properties);

        XianyuClientException first = assertThrows(XianyuClientException.class,
                () -> client.execute(XianyuReadEndpoint.AUTHORIZED_SHOPS, null));
        assertEquals(XianyuClientException.Kind.INTEGRATION_DISABLED, first.getKind());

        mockWebServer.enqueue(new MockResponse().setBody("{\"code\":0,\"msg\":\"OK\",\"data\":{\"list\":[]}}"));
        client.execute(XianyuReadEndpoint.AUTHORIZED_SHOPS, null);
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void shouldUseOfficialSigningFormulaAndRedactPrivateFields() {
        assertEquals("e427ba8f137c0560e18a2fb12e13e745",
                requestSigner.sign("demo-app", "demo-secret", FIXED_TIMESTAMP, "{\"product_id\":\"123\"}"));

        String redacted = new XianyuLogRedactor(objectMapper).redactJson(
                "{\"appSecret\":\"do-not-log\",\"receiver_mobile\":\"13800138000\",\"address\":\"Road 1\",\"safe\":\"ok\"}");
        assertEquals("{\"appSecret\":\"***\",\"receiver_mobile\":\"***\",\"address\":\"***\",\"safe\":\"ok\"}", redacted);
    }

    private XianyuProperties readyProperties() {
        XianyuProperties result = new XianyuProperties();
        result.setEnabled(true);
        result.setAppKey("demo-app");
        result.setAppSecret("demo-secret");
        return result;
    }

}
