package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 快递100 运单号智能识别（autoComNum），根据运单号推断可能的承运商。
 * 该接口无需签名，识别结果仅供运营参考，最终以人工确认为准。
 */
@Component
public class Kuaidi100AutoNumberClient {

    static final String DEFAULT_AUTO_NUMBER_URL = "https://www.kuaidi100.com/autonumber/autoComNum";
    static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String autoNumberUrl;

    @Autowired
    public Kuaidi100AutoNumberClient(@Qualifier("kuaidi100OkHttpClient") OkHttpClient client,
                                     ObjectMapper objectMapper) {
        this(client, objectMapper, DEFAULT_AUTO_NUMBER_URL);
    }

    public Kuaidi100AutoNumberClient(OkHttpClient client, ObjectMapper objectMapper, String autoNumberUrl) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.autoNumberUrl = autoNumberUrl;
    }

    public List<Kuaidi100AutoNumberCandidate> recognize(String waybillNo) throws IOException {
        HttpUrl url = HttpUrl.get(autoNumberUrl).newBuilder()
                .addQueryParameter("resultv2", "1")
                .addQueryParameter("text", waybillNo)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("Referer", "https://www.kuaidi100.com/")
                .header("User-Agent", USER_AGENT)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("KUAIDI100_AUTO_NUMBER_HTTP_" + response.code());
            }
            return parse(response.body().string());
        }
    }

    List<Kuaidi100AutoNumberCandidate> parse(String body) throws IOException {
        JsonNode auto = objectMapper.readTree(body).path("auto");
        if (!auto.isArray()) {
            return List.of();
        }
        List<Kuaidi100AutoNumberCandidate> result = new ArrayList<>(auto.size());
        for (JsonNode item : auto) {
            String comCode = item.path("comCode").asText("").trim();
            if (comCode.isEmpty()) {
                continue;
            }
            result.add(new Kuaidi100AutoNumberCandidate(comCode, item.path("name").asText("").trim()));
        }
        return result;
    }

}
