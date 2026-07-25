package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuProductListPageParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XianyuProductListPageParser parser = new XianyuProductListPageParser();

    @Test
    void shouldParseDocumentedProductListPage() throws Exception {
        XianyuProductListPage page = parser.parse(response("""
                {"code":0,"msg":"OK","data":{"list":[
                {"product_id":448592974859525,"update_time":1691657199,"spec_type":2}
                ],"count":1,"page_no":1,"page_size":50}}
                """));

        assertEquals(1, page.count());
        assertEquals("448592974859525", page.entries().get(0).externalProductId());
        assertEquals(2, page.entries().get(0).specType());
        assertEquals(LocalDateTime.of(2023, 8, 10, 16, 46, 39),
                page.entries().get(0).sourceUpdatedAt());
    }

    @Test
    void shouldRejectPageSizeOutsideDocumentedBounds() throws Exception {
        XianyuReadResponse response = response("""
                {"code":0,"msg":"OK","data":{"list":[],"count":0,"page_no":1,"page_size":101}}
                """);

        assertThrows(RuntimeException.class, () -> parser.parse(response));
    }

    private XianyuReadResponse response(String rawBody) throws Exception {
        return new XianyuReadResponse(200, 0, objectMapper.readTree(rawBody), rawBody);
    }

}
