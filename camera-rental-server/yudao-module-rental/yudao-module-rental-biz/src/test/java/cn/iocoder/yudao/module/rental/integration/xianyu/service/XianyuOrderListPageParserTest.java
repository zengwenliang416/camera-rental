package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuOrderListPageParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XianyuOrderListPageParser parser = new XianyuOrderListPageParser();

    @Test
    void shouldRejectFractionalPaginationMetadata() throws Exception {
        assertThrows(RuntimeException.class, () -> parser.parse(response("""
                {"code":0,"data":{"count":1.5,"page_no":1,"page_size":50,"list":[]}}""")));
    }

    @Test
    void shouldRejectZeroPageSize() throws Exception {
        assertThrows(RuntimeException.class, () -> parser.parse(response("""
                {"code":0,"data":{"count":0,"page_no":0,"page_size":0,"list":[]}}""")));
    }

    private XianyuReadResponse response(String json) throws Exception {
        JsonNode payload = objectMapper.readTree(json);
        return new XianyuReadResponse(200, 0, payload, json);
    }

}
