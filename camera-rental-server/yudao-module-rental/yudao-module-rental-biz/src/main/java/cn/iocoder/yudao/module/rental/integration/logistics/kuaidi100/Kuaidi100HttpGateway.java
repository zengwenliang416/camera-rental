package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class Kuaidi100HttpGateway implements Kuaidi100Gateway {

    static final String DEFAULT_SUBSCRIBE_URL = "https://poll.kuaidi100.com/poll";
    static final String DEFAULT_QUERY_URL = "https://poll.kuaidi100.com/poll/query.do";

    private final OkHttpClient client;
    private final String subscribeUrl;
    private final String queryUrl;

    public Kuaidi100HttpGateway(@Qualifier("kuaidi100OkHttpClient") OkHttpClient client) {
        this(client, DEFAULT_SUBSCRIBE_URL, DEFAULT_QUERY_URL);
    }

    public Kuaidi100HttpGateway(OkHttpClient client, String subscribeUrl, String queryUrl) {
        this.client = client;
        this.subscribeUrl = subscribeUrl;
        this.queryUrl = queryUrl;
    }

    @Override
    public String subscribe(Map<String, String> form) throws IOException {
        return post(subscribeUrl, form);
    }

    @Override
    public String query(Map<String, String> form) throws IOException {
        return post(queryUrl, form);
    }

    private String post(String url, Map<String, String> form) throws IOException {
        FormBody.Builder body = new FormBody.Builder();
        form.forEach(body::add);
        Request request = new Request.Builder()
                .url(url)
                .post(body.build())
                .header("Accept", "application/json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("KUAIDI100_HTTP_" + response.code());
            }
            return response.body().string();
        }
    }
}
