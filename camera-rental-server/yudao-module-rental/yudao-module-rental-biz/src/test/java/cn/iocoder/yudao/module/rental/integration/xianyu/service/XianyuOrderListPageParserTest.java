package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void shouldParseEntriesWithSellerRemark() throws Exception {
        XianyuOrderListPage page = parser.parse(response("""
                {"code":0,"data":{"count":2,"page_no":1,"page_size":50,"list":[
                {"order_no":"order-1","update_time":1784710800,"seller_remark":"发货9.02 收货9.03 发回9.05"},
                {"order_no":"order-2","update_time":1784714400}]}}"""));

        assertEquals(2, page.entries().size());
        XianyuOrderListEntry withRemark = page.entries().get(0);
        assertEquals("order-1", withRemark.externalOrderId());
        assertEquals(LocalDateTime.of(2026, 7, 22, 17, 0), withRemark.sourceUpdatedAt());
        assertEquals("发货9.02 收货9.03 发回9.05", withRemark.sellerRemark());
        assertNull(page.entries().get(1).sellerRemark());
    }

    private XianyuReadResponse response(String json) throws Exception {
        JsonNode payload = objectMapper.readTree(json);
        return new XianyuReadResponse(200, 0, payload, json);
    }

}
