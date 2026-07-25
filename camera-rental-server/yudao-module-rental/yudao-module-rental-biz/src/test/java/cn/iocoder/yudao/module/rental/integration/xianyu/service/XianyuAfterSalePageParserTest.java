package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianyuAfterSalePageParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XianyuAfterSalePageParser parser = new XianyuAfterSalePageParser(objectMapper);

    @Test
    void shouldParseDocumentedAfterSaleListShape() throws Exception {
        XianyuAfterSalePage page = parser.parse(response("""
                {"code":0,"data":{"list":[{"refund_no":"393611004596913055",
                "order_no":"5119489332694004337","refund_status":3,"refund_amount":1200,
                "apply_time":1784774400,"refund_time":1784860800,"timeout_time":1784947200}],
                "has_next_page":false}}"""));

        assertEquals(1, page.entries().size());
        XianyuAfterSaleSnapshot snapshot = page.entries().get(0);
        assertEquals("393611004596913055", snapshot.externalAfterSaleId());
        assertEquals("5119489332694004337", snapshot.externalOrderId());
        assertEquals("3", snapshot.afterSaleStatus());
        assertEquals(1200L, snapshot.refundAmount());
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochSecond(1784860800),
                ZoneId.of("Asia/Shanghai")), snapshot.sourceUpdatedAt());
        assertTrue(snapshot.payloadJson().contains("refund_no"));
    }

    @Test
    void shouldRejectMissingList() throws Exception {
        assertThrows(RuntimeException.class, () -> parser.parse(response("{\"code\":0,\"data\":{}}")));
    }

    private XianyuReadResponse response(String json) throws Exception {
        JsonNode payload = objectMapper.readTree(json);
        return new XianyuReadResponse(200, 0, payload, json);
    }

}
