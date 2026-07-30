package cn.iocoder.yudao.module.rental.integration.xianyu.client;

import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XianyuWriteClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer server;
    private XianyuRuntimeConfigService runtimeConfigService;
    private XianyuProperties properties;
    private XianyuWriteClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        properties = readyProperties();
        properties.setBaseUrl(server.url("/").toString());
        runtimeConfigService = mock(XianyuRuntimeConfigService.class);
        when(runtimeConfigService.getCurrent()).thenReturn(properties);
        client = new XianyuWriteClient(runtimeConfigService, new XianyuCanonicalJson(objectMapper),
                new XianyuRequestSigner(), new OkHttpClient(), objectMapper,
                Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void shouldRejectDisabledWriteSwitchBeforeNetworkCall() {
        properties.setWriteEnabled(false);

        XianyuClientException exception = assertThrows(XianyuClientException.class,
                () -> client.execute(XianyuWriteEndpoint.ORDER_SHIP,
                        objectMapper.createObjectNode().put("order_no", "test-order")));

        assertEquals(XianyuClientException.Kind.WRITE_DISABLED, exception.getKind());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void shouldResolvePersistedConfigurationForEveryWriteRequest() {
        XianyuProperties writeDisabled = readyProperties();
        writeDisabled.setBaseUrl(server.url("/").toString());
        writeDisabled.setWriteEnabled(false);
        when(runtimeConfigService.getCurrent()).thenReturn(writeDisabled, properties);

        assertThrows(XianyuClientException.class,
                () -> client.execute(XianyuWriteEndpoint.ORDER_SHIP, objectMapper.createObjectNode()));

        server.enqueue(new MockResponse().setBody("{\"code\":0,\"msg\":\"OK\",\"data\":{}}"));
        client.execute(XianyuWriteEndpoint.ORDER_SHIP, objectMapper.createObjectNode());

        assertEquals(1, server.getRequestCount());
    }

    private XianyuProperties readyProperties() {
        XianyuProperties result = new XianyuProperties();
        result.setEnabled(true);
        result.setAppKey("demo-app");
        result.setAppSecret("demo-secret");
        result.setWriteEnabled(true);
        result.setTenantId(9L);
        return result;
    }

}
