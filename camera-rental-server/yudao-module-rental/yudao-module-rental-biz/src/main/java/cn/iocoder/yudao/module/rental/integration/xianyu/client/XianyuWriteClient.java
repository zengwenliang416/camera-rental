package cn.iocoder.yudao.module.rental.integration.xianyu.client;

import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Backend-only client for the documented XianGuanJia write endpoint allowlist.
 */
public class XianyuWriteClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final XianyuProperties properties;
    private final XianyuCanonicalJson canonicalJson;
    private final XianyuRequestSigner requestSigner;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public XianyuWriteClient(XianyuProperties properties, XianyuCanonicalJson canonicalJson,
                             XianyuRequestSigner requestSigner, OkHttpClient httpClient,
                             ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.canonicalJson = canonicalJson;
        this.requestSigner = requestSigner;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public XianyuReadResponse execute(XianyuWriteEndpoint endpoint, JsonNode body) {
        assertReady();
        String bodyString = canonicalJson.serialize(body);
        long timestamp = clock.instant().getEpochSecond();
        String signature = requestSigner.sign(properties.getAppKey(), properties.getAppSecret(), timestamp, bodyString);
        HttpUrl url = buildUrl(endpoint, timestamp, signature);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyString.getBytes(StandardCharsets.UTF_8), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String rawBody = readBody(response.body());
            if (!response.isSuccessful()) {
                throw new XianyuClientException(XianyuClientException.Kind.HTTP_STATUS,
                        "XianGuanJia returned an HTTP error", response.code(), null);
            }
            JsonNode payload = parseResponse(rawBody);
            if (!payload.path("code").canConvertToInt()) {
                throw new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE,
                        "XianGuanJia response is missing a numeric code");
            }
            int remoteCode = payload.path("code").intValue();
            if (remoteCode != 0) {
                throw new XianyuClientException(XianyuClientException.Kind.REMOTE_RESPONSE,
                        "XianGuanJia returned a remote error", response.code(), remoteCode);
            }
            return new XianyuReadResponse(response.code(), remoteCode, payload, rawBody);
        } catch (IOException exception) {
            throw new XianyuClientException(XianyuClientException.Kind.TRANSPORT,
                    "Unable to reach XianGuanJia", exception);
        }
    }

    private void assertReady() {
        switch (properties.getIntegrationStatus()) {
            case DISABLED -> throw new XianyuClientException(XianyuClientException.Kind.INTEGRATION_DISABLED,
                    "XianGuanJia integration is disabled");
            case MISSING_CREDENTIALS -> throw new XianyuClientException(XianyuClientException.Kind.MISSING_CREDENTIALS,
                    "XianGuanJia runtime credentials are missing");
            case READY -> {
                // Continue.
            }
        }
    }

    private HttpUrl buildUrl(XianyuWriteEndpoint endpoint, long timestamp, String signature) {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        try {
            return HttpUrl.get(baseUrl + endpoint.getPath())
                    .newBuilder()
                    .addQueryParameter("appid", properties.getAppKey())
                    .addQueryParameter("timestamp", Long.toString(timestamp))
                    .addQueryParameter("sign", signature)
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new XianyuClientException(XianyuClientException.Kind.MALFORMED_REQUEST,
                    "XianGuanJia base URL is invalid", exception);
        }
    }

    private String readBody(ResponseBody body) throws IOException {
        return body == null ? "" : body.string();
    }

    private JsonNode parseResponse(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (JsonProcessingException exception) {
            throw new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE,
                    "XianGuanJia returned malformed JSON", exception);
        }
    }

}
