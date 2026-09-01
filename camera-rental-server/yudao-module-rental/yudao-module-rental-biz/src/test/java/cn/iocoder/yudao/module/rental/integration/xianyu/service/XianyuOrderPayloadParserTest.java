package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuOrderPayloadParserTest {

    private final XianyuOrderPayloadParser parser = new XianyuOrderPayloadParser(
            new ObjectMapper(), new XianyuChannelIdentifierNormalizer());

    @Test
    void shouldParseDocumentedOrderDetailFieldsInShanghaiTime() {
        XianyuOrderSnapshot snapshot = parser.parse("""
                {"code":0,"data":{"order_no":"3364202298717566229","order_status":22,
                "pay_amount":16000,"seller_remark":"#租期1.2-1.4#","create_time":1704067200,
                "update_time":1704153600,"receiver_name":"张三","receiver_mobile":"13800138000",
                "prov_name":"广东省","city_name":"深圳市","area_name":"南山区",
                "town_name":"粤海街道","address":"桂庙新村100室",
                "goods":{"product_id":421611860404485,"item_id":1062409679830,
                "sku_id":5146011339969}}}""");

        assertEquals("3364202298717566229", snapshot.externalOrderId());
        assertEquals("421611860404485", snapshot.xgjProductId());
        assertEquals("1062409679830", snapshot.xianyuItemId());
        assertEquals("5146011339969", snapshot.xgjSkuId());
        assertEquals("22", snapshot.orderStatus());
        assertEquals(16000L, snapshot.payAmount());
        assertEquals("#租期1.2-1.4#", snapshot.sellerRemark());
        assertEquals("张三", snapshot.receiverName());
        assertEquals("13800138000", snapshot.receiverMobile());
        assertEquals("广东省深圳市南山区粤海街道桂庙新村100室", snapshot.receiverAddress());
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

    @Test
    void shouldNormalizeBlankOptionalLogisticsFieldsToNull() {
        XianyuOrderSnapshot snapshot = parser.parse("""
                {"code":0,"data":{"order_no":"3364342266781566229","order_status":12,
                "pay_amount":4350,"waybill_no":"","express_code":"  ","express_name":"",
                "goods":{"product_id":421611860506885,"sku_id":0}}}""");

        assertNull(snapshot.waybillNo());
        assertNull(snapshot.expressCode());
        assertNull(snapshot.expressName());
    }

    @Test
    void shouldNotFallbackBetweenProductIdentifiers() {
        XianyuOrderSnapshot snapshot = parser.parse("""
                {"code":0,"data":{"order_no":"order-item-only",
                "goods":{"item_id":"1062409679830"}}}
                """);

        assertNull(snapshot.xgjProductId());
        assertEquals("1062409679830", snapshot.xianyuItemId());
        assertNull(snapshot.xgjSkuId());
    }

}
