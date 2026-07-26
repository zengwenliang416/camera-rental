package cn.iocoder.yudao.module.rental.integration.ocr;

import cn.iocoder.yudao.module.rental.integration.ocr.config.ShipmentOcrProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenAiCompatibleShipmentOcrClient implements ShipmentOcrClient {

    static final String PROMPT = """
            Extract shipment information from this image.
            Return strict JSON only: {"waybill_no":"","express_name":"","confidence":0.0}.
            Only extract a courier waybill number and courier company name.
            Handle both physical courier-label photos and courier app screenshots.
            If a one-dimensional barcode and a large printed waybill number are visible, prefer that barcode/large number.
            Ignore unrelated order numbers, phone numbers, dates, prices, and background list cards.
            If uncertain or no waybill is visible, return empty strings and confidence 0.
            """;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ShipmentOcrProperties properties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleShipmentOcrClient(ShipmentOcrProperties properties,
                                             OkHttpClient okHttpClient,
                                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ShipmentOcrResult extract(MultipartFile file) {
        if (!isReady() || file == null || file.isEmpty()) {
            return ShipmentOcrResult.empty("AI_MULTIMODAL_DISABLED");
        }
        try {
            Request request = new Request.Builder()
                    .url(chatCompletionsUrl())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(requestBody(file)), JSON))
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ShipmentOcrResult.empty("AI_MULTIMODAL_HTTP_" + response.code());
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    return ShipmentOcrResult.empty("AI_MULTIMODAL_EMPTY");
                }
                return parseResponse(responseBody.string());
            }
        } catch (Exception ignored) {
            return ShipmentOcrResult.empty("AI_MULTIMODAL_ERROR");
        }
    }

    private boolean isReady() {
        return properties != null
                && properties.isEnable()
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }

    private Map<String, Object> requestBody(MultipartFile file) throws Exception {
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "image/jpeg";
        String imageUrl = "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(file.getBytes());
        return Map.of(
                "model", properties.getModel(),
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", "You extract courier waybill data and return JSON only."),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", PROMPT),
                                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))))));
    }

    private String chatCompletionsUrl() {
        String baseUrl = properties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/chat/completions";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private ShipmentOcrResult parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode contentNode = root.at("/choices/0/message/content");
        if (!contentNode.isTextual()) {
            return ShipmentOcrResult.empty("AI_MULTIMODAL_EMPTY_CONTENT");
        }
        JsonNode result = objectMapper.readTree(extractJsonObject(contentNode.asText()));
        return new ShipmentOcrResult(
                text(result, "waybill_no", "waybillNo"),
                text(result, "express_name", "expressName"),
                confidence(result),
                "AI_MULTIMODAL");
    }

    private static String extractJsonObject(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return "{}";
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isTextual()) {
                return value.asText().trim().toUpperCase(Locale.ROOT);
            }
        }
        return "";
    }

    private static BigDecimal confidence(JsonNode node) {
        JsonNode value = node.get("confidence");
        if (value == null || !value.isNumber()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value.asDouble()).max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

}
