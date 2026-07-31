package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsQueryCommand;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsSubscribeCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderConfigService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderCredentialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Kuaidi100LogisticsProviderTest {

    private final RentalLogisticsProviderConfigService configService =
            mock(RentalLogisticsProviderConfigService.class);
    private final RentalLogisticsProviderCredentialService credentialService =
            mock(RentalLogisticsProviderCredentialService.class);
    private MockWebServer server;
    private Kuaidi100LogisticsProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        ObjectMapper objectMapper = new ObjectMapper();
        Kuaidi100Gateway gateway = new Kuaidi100HttpGateway(new OkHttpClient(),
                server.url("/subscribe").toString(), server.url("/query").toString());
        provider = new Kuaidi100LogisticsProvider(configService, credentialService,
                gateway, new Kuaidi100Signer(),
                new Kuaidi100Converter(objectMapper, new Kuaidi100StatusMapper()), objectMapper);
        when(configService.get("KUAIDI100")).thenReturn(RentalLogisticsProviderConfigDO.builder()
                .enabled(true)
                .queryEnabled(true)
                .subscribeEnabled(true)
                .callbackSecret("fixture-salt")
                .resultVersion("4")
                .build());
        RentalLogisticsProviderCredentialDO credential = RentalLogisticsProviderCredentialDO.builder()
                .id(7L)
                .providerCode("KUAIDI100")
                .enabled(true)
                .customerCode("fixture-customer")
                .apiKey("fixture-key")
                .build();
        when(credentialService.get(7L)).thenReturn(credential);
        when(credentialService.isUsable(credential, "KUAIDI100")).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsOfficialQueryFormAndConvertsTrace() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"status":"200","state":"3","data":[
                          {"time":"2026-07-31 09:00:00","context":"已揽收","location":"长沙","status":"1"},
                          {"time":"2026-07-31 12:00:00","context":"已签收","location":"上海","status":"3"}
                        ]}
                        """));

        LogisticsOperationResult result = provider.query(
                new LogisticsQueryCommand(1L, 7L, "shunfeng", "SF0000000001", "13800000000"));

        assertTrue(result.successful());
        assertNotNull(result.snapshot());
        assertEquals(2, result.snapshot().events().size());
        assertEquals("DELIVERED", result.snapshot().events().get(1).trackingStatus().name());
        RecordedRequest request = server.takeRequest();
        assertEquals("/query", request.getPath());
        Map<String, String> form = decodeForm(request.getBody().readUtf8());
        assertEquals("fixture-customer", form.get("customer"));
        assertEquals(new Kuaidi100Signer().signQuery(form.get("param"), "fixture-key", "fixture-customer"),
                form.get("sign"));
    }

    @Test
    void sendsOfficialSubscriptionEnvelope() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":true,\"returnCode\":\"200\",\"message\":\"提交成功\"}"));

        LogisticsOperationResult result = provider.subscribe(new LogisticsSubscribeCommand(
                1L, 7L, "shunfeng", "SF0000000001", null,
                "https://example.test/callback/token", "fixture-delivery-salt"));

        assertTrue(result.successful());
        RecordedRequest request = server.takeRequest();
        assertEquals("/subscribe", request.getPath());
        Map<String, String> form = decodeForm(request.getBody().readUtf8());
        assertEquals("json", form.get("schema"));
        assertTrue(form.get("param").contains("\"key\":\"fixture-key\""));
        assertTrue(form.get("param").contains("\"salt\":\"fixture-delivery-salt\""));
        assertTrue(form.get("param").contains("\"resultv2\":\"4\""));
    }

    @Test
    void subscriptionRequiresApiKeyButNotQueryCustomerCode() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":true,\"returnCode\":\"200\"}"));

        LogisticsOperationResult result = provider.subscribe(new LogisticsSubscribeCommand(
                1L, 7L, "shunfeng", "SF0000000001", null,
                "https://example.test/callback/token", "fixture-delivery-salt"));

        assertTrue(result.successful());
        assertEquals("/subscribe", server.takeRequest().getPath());
    }

    @Test
    void queryReturnsStableReasonWhenCredentialIsMissing() {
        when(credentialService.get(99L)).thenReturn(null);

        LogisticsOperationResult result = provider.query(
                new LogisticsQueryCommand(1L, 99L, "shunfeng", "SF0000000001", null));

        assertEquals(false, result.successful());
        assertEquals("PROVIDER_CREDENTIAL_REQUIRED", result.safeCode());
        assertEquals(0, server.getRequestCount());
    }

    private Map<String, String> decodeForm(String raw) {
        return Arrays.stream(raw.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }
}
