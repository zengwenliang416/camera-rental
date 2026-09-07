package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.service.rental.RentalAddressParseResult;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Kuaidi100AddressResolutionClientTest {

    private MockWebServer server;
    private Kuaidi100AddressResolutionClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        Kuaidi100Gateway gateway = new Kuaidi100HttpGateway(new OkHttpClient(),
                server.url("/subscribe").toString(), server.url("/query").toString(),
                server.url("/address/resolution").toString());
        client = new Kuaidi100AddressResolutionClient(gateway, new Kuaidi100Signer(), new ObjectMapper(),
                Clock.fixed(Instant.ofEpochMilli(1_788_763_000_000L), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void signsRequestAndNormalizesMunicipalityAddress() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"code":200,"message":"success","data":{
                          "name":"Nicole","mobile":["13800000000"],
                          "province":"上海","city":"上海","district":"松江区",
                          "fourth":"永丰街道","address":"秀庭酒店(上海松江站店)"
                        }}
                        """));
        RentalLogisticsProviderCredentialDO credential = RentalLogisticsProviderCredentialDO.builder()
                .apiKey("fixture-key").apiSecret("fixture-secret").build();

        RentalAddressParseResult result = client.resolve(
                "Nicole，13800000000，上海上海市松江区永丰街道秀庭酒店(上海松江站店)", credential);

        assertEquals("Nicole", result.name());
        assertEquals("13800000000", result.mobile());
        assertEquals("上海市松江区永丰街道秀庭酒店(上海松江站店)", result.address());
        assertFalse(result.fallback());
        RecordedRequest request = server.takeRequest();
        assertEquals("/address/resolution", request.getPath());
        String rawForm = request.getBody().readUtf8();
        Map<String, String> form = decodeForm(rawForm);
        assertEquals("fixture-key", form.get("key"));
        assertEquals("1788763000000", form.get("t"));
        assertEquals(new Kuaidi100Signer().signAddress(form.get("param"), form.get("t"),
                "fixture-key", "fixture-secret"), form.get("sign"));
        assertFalse(rawForm.contains("fixture-secret"));
    }

    @Test
    void multipleMobilesRequireManualReview() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":200,\"data\":{\"mobile\":[\"13800000000\",\"13900000000\"]}}"));
        RentalAddressParseResult result = client.resolve("联系人 13800000000 13900000000",
                RentalLogisticsProviderCredentialDO.builder()
                        .apiKey("fixture-key").apiSecret("fixture-secret").build());

        assertNull(result.mobile());
        assertTrue(result.warnings().contains("MULTIPLE_MOBILES_REVIEW"));
    }

    private Map<String, String> decodeForm(String raw) {
        return Arrays.stream(raw.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }
}
