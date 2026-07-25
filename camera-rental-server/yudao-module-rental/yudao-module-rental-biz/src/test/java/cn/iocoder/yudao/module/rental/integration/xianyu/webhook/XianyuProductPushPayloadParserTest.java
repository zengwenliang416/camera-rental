package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuProductPushPayloadParserTest {

    private final XianyuProductPushPayloadParser parser =
            new XianyuProductPushPayloadParser(new ObjectMapper());

    @Test
    void shouldParseDocumentedProductPushPayload() {
        XianyuProductPushPayload payload = parser.parse(validPayload());

        assertEquals("123456", payload.sellerId());
        assertEquals("441160510721413", payload.externalProductId());
        assertEquals(22, payload.productStatus());
        assertEquals(3, payload.publishStatus());
        assertEquals(2, payload.itemBizType());
        assertEquals(5500L, payload.price());
        assertEquals(1, payload.stock());
        assertEquals(1694000092L, payload.modifyTime());
    }

    @Test
    void shouldRejectStringSellerId() {
        String payload = validPayload().replace("123456", "\"123456\"");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    @Test
    void shouldRejectUnknownProductStatus() {
        String payload = validPayload().replace("\"product_status\":22", "\"product_status\":99");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    @Test
    void shouldRejectOutOfRangePrice() {
        String payload = validPayload().replace("\"price\":5500", "\"price\":10000000000");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    @Test
    void shouldRejectMissingUserName() {
        String payload = validPayload().replace("\"user_name\":\"tb924343042\",", "");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(payload));
    }

    private String validPayload() {
        return """
                {
                  "seller_id":123456,
                  "product_id":441160510721413,
                  "product_status":22,
                  "publish_status":3,
                  "item_biz_type":2,
                  "price":5500,
                  "stock":1,
                  "user_name":"tb924343042",
                  "modify_time":1694000092
                }
                """;
    }

}
