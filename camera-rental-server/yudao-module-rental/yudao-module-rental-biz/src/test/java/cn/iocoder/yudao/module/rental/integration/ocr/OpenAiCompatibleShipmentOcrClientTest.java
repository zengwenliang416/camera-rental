package cn.iocoder.yudao.module.rental.integration.ocr;

import cn.iocoder.yudao.module.rental.integration.ocr.config.ShipmentOcrProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleShipmentOcrClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSendBase64ImageUrlToOpenAiCompatibleRelay() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {"choices":[{"message":{"content":"{\\"waybill_no\\":\\"SF5113560342626\\",\\"express_name\\":\\"顺丰速运\\",\\"confidence\\":0.92}"}}]}
                            """));
            server.start();
            ShipmentOcrProperties properties = new ShipmentOcrProperties();
            properties.setEnable(true);
            properties.setBaseUrl(server.url("/v1").toString());
            properties.setApiKey("relay-test-key");
            properties.setModel("vision-relay-model");
            OpenAiCompatibleShipmentOcrClient client = new OpenAiCompatibleShipmentOcrClient(
                    properties, new OkHttpClient(), objectMapper);

            ShipmentOcrResult result = client.extract(new MockMultipartFile(
                    "file", "waybill.jpg", "image/jpeg", "fake-image".getBytes()));

            assertEquals("SF5113560342626", result.getWaybillNo());
            assertEquals("顺丰速运", result.getExpressName());
            assertEquals(new BigDecimal("0.92"), result.getConfidence());
            assertEquals("AI_MULTIMODAL", result.getSource());
            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("/v1/chat/completions", request.getPath());
            assertEquals("Bearer relay-test-key", request.getHeader("Authorization"));
            JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
            assertEquals("vision-relay-model", body.get("model").asText());
            String requestJson = body.toString();
            assertTrue(requestJson.contains(OpenAiCompatibleShipmentOcrClient.PROMPT.substring(0, 32)));
            assertTrue(requestJson.contains("\"type\":\"image_url\""));
            assertTrue(requestJson.contains("data:image/jpeg;base64,ZmFrZS1pbWFnZQ=="));
        }
    }

    @Test
    void shouldDegradeToEmptyWhenRelayIsDisabled() {
        ShipmentOcrProperties properties = new ShipmentOcrProperties();
        OpenAiCompatibleShipmentOcrClient client = new OpenAiCompatibleShipmentOcrClient(
                properties, new OkHttpClient(), objectMapper);

        ShipmentOcrResult result = client.extract(new MockMultipartFile(
                "file", "waybill.jpg", "image/jpeg", "fake-image".getBytes()));

        assertEquals("AI_MULTIMODAL_DISABLED", result.getSource());
        assertEquals(BigDecimal.ZERO, result.getConfidence());
    }

}
