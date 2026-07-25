package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuOrderPushPayloadParserTest {

    private final XianyuOrderPushPayloadParser parser =
            new XianyuOrderPushPayloadParser(new ObjectMapper());

    @Test
    void shouldParseDocumentedStrongTypes() {
        XianyuOrderPushPayload payload = parser.parse(validPayload());

        assertEquals("123456", payload.sellerId());
        assertEquals("order-1", payload.externalOrderId());
        assertEquals(22, payload.orderStatus());
        assertEquals(5, payload.refundStatus());
        assertEquals(1784890000L, payload.modifyTime());
    }

    @Test
    void shouldRejectNumericOrderNumber() {
        String payload = validPayload().replace("\"order-1\"", "123");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    @Test
    void shouldRejectStringSellerId() {
        String payload = validPayload().replace("123456", "\"123456\"");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    @Test
    void shouldRejectUnknownStatus() {
        String payload = validPayload().replace("\"order_status\":22", "\"order_status\":99");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    @Test
    void shouldRejectIntegerOutsideLongRange() {
        String payload = validPayload().replace("987654", "999999999999999999999999");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    @Test
    void shouldRejectOrderNumberBeyondStorageBoundary() {
        String payload = validPayload().replace("order-1", "x".repeat(129));

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    private String validPayload() {
        return """
                {
                  "seller_id":123456,
                  "user_name":"masked-in-storage",
                  "order_no":"order-1",
                  "order_type":1,
                  "order_status":22,
                  "refund_status":5,
                  "modify_time":1784890000,
                  "product_id":987654,
                  "item_id":876543
                }
                """;
    }

}
