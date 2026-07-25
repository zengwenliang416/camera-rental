package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuProductDetailPayloadParserTest {

    private final XianyuProductDetailPayloadParser parser =
            new XianyuProductDetailPayloadParser(new ObjectMapper());

    @Test
    void shouldParseSuccessfulEnvelope() {
        XianyuProductSnapshot snapshot = parser.parse(successfulResponse());

        assertEquals("441160510721413", snapshot.externalProductId());
        assertEquals("Sony A7M4", snapshot.title());
        assertEquals("fee623cbc89d0ab7a7f7e02f36b0b49a", snapshot.categoryId());
        assertEquals("22", snapshot.status());
        assertEquals(LocalDateTime.of(2023, 9, 6, 19, 34, 52), snapshot.sourceUpdatedAt());
    }

    @Test
    void shouldRejectRemoteErrorEnvelope() {
        assertThrows(RuntimeException.class, () -> parser.parse("{\"code\":500,\"msg\":\"fail\"}"));
    }

    private String successfulResponse() {
        return """
                {
                  "code":0,
                  "msg":"OK",
                  "data":{
                    "product_id":441160510721413,
                    "product_status":22,
                    "channel_cat_id":"fee623cbc89d0ab7a7f7e02f36b0b49a",
                    "title":"Sony A7M4",
                    "price":5500,
                    "stock":1,
                    "publish_status":3,
                    "update_time":1694000092
                  }
                }
                """;
    }

}
