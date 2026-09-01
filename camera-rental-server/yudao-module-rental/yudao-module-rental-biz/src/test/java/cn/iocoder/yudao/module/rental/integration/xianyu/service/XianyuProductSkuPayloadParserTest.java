package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuProductSkuPayloadParserTest {

    private final XianyuProductSkuPayloadParser parser =
            new XianyuProductSkuPayloadParser(
                    new ObjectMapper(), new XianyuChannelIdentifierNormalizer());

    @Test
    void shouldParseDocumentedSkuPayload() {
        List<XianyuProductSkuGroup> groups = parser.parse(response());

        assertEquals("537044127563781", groups.get(0).xgjProductId());
        assertEquals("537044127563786", groups.get(0).skuItems().get(0).xgjSkuId());
        assertEquals("942506886325", groups.get(0).skuItems().get(0).xianyuSkuId());
        assertEquals("颜色:蓝色", groups.get(0).skuItems().get(0).skuName());
        assertEquals(1, groups.get(0).skuItems().get(0).stock());
    }

    @Test
    void shouldRejectMalformedSkuItems() {
        String payload = response().replace("\"sku_items\":[", "\"sku_items\":{");

        assertThrows(RuntimeException.class, () -> parser.parse(payload));
    }

    private String response() {
        return """
                {"code":0,"msg":"OK","data":{"list":[{"product_id":537044127563781,
                "sku_items":[{"sku_id":537044127563786,"xy_sku_id":942506886325,
                "price":2,"stock":1,
                "sku_text":"颜色:蓝色","outer_id":"gyfbcs240416001"}]}]}}
                """;
    }

}
