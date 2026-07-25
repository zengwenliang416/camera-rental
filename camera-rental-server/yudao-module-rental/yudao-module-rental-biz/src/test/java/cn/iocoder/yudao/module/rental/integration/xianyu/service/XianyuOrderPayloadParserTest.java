package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuOrderPayloadParserTest {

    private final XianyuOrderPayloadParser parser = new XianyuOrderPayloadParser(new ObjectMapper());

    @Test
    void shouldParseDocumentedOrderDetailFieldsInShanghaiTime() {
        XianyuOrderSnapshot snapshot = parser.parse("""
                {"code":0,"data":{"order_no":"3364202298717566229","order_status":22,
                "pay_amount":16000,"seller_remark":"#租期1.2-1.4#","create_time":1704067200,
                "update_time":1704153600,"goods":{"product_id":421611860404485,
                "sku_id":5146011339969}}}""");

        assertEquals("3364202298717566229", snapshot.externalOrderId());
        assertEquals("421611860404485", snapshot.externalProductId());
        assertEquals("5146011339969", snapshot.externalSkuId());
        assertEquals("22", snapshot.orderStatus());
        assertEquals(16000L, snapshot.payAmount());
        assertEquals("#租期1.2-1.4#", snapshot.sellerRemark());
        assertEquals(LocalDateTime.of(2024, 1, 1, 8, 0), snapshot.sourceCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 2, 8, 0), snapshot.sourceUpdatedAt());
        org.junit.jupiter.api.Assertions.assertTrue(snapshot.detailJson().contains("\"order_no\":\"3364202298717566229\""));
        org.junit.jupiter.api.Assertions.assertTrue(snapshot.goodsJson().contains("product_id"));
    }

    @Test
    void shouldRejectNonSuccessAndMissingOrderNumber() {
        assertThrows(XianyuClientException.class, () -> parser.parse("{\"code\":100001,\"data\":{}}"));
        assertThrows(XianyuClientException.class, () -> parser.parse("{\"code\":0,\"data\":{\"pay_amount\":1}}"));
    }

}
