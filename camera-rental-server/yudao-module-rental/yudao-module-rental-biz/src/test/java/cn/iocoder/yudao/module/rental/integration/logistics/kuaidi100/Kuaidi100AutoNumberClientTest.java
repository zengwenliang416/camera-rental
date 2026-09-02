package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Kuaidi100AutoNumberClientTest {

    private MockWebServer server;
    private Kuaidi100AutoNumberClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new Kuaidi100AutoNumberClient(new OkHttpClient(), new ObjectMapper(),
                server.url("/autonumber").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsAutoNumberQueryAndParsesCandidates() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"comCode":"","num":"SF5119694772350",
                         "auto":[{"comCode":"shunfeng","lengthPre":15,"name":"顺丰速运"}],
                         "autoDest":[{"comCode":"shunfeng","name":"顺丰速运"}]}
                        """));

        List<Kuaidi100AutoNumberCandidate> candidates = client.recognize("SF5119694772350");

        assertEquals(1, candidates.size());
        assertEquals("shunfeng", candidates.get(0).comCode());
        assertEquals("顺丰速运", candidates.get(0).name());
        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/autonumber?resultv2=1&text=SF5119694772350", request.getPath());
        assertEquals("https://www.kuaidi100.com/", request.getHeader("Referer"));
    }

    @Test
    void skipsCandidatesWithoutComCode() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"comCode":"","num":"73512345678901",
                         "auto":[{"comCode":"","name":""},{"comCode":"zhongtong","name":"中通快递"}]}
                        """));

        List<Kuaidi100AutoNumberCandidate> candidates = client.recognize("73512345678901");

        assertEquals(1, candidates.size());
        assertEquals("zhongtong", candidates.get(0).comCode());
    }

    @Test
    void returnsEmptyWhenNumberIsInvalid() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"returnCode\":\"201\",\"message\":\"不是有效的快递单号\",\"result\":false}"));

        assertTrue(client.recognize("abc").isEmpty());
    }

}
