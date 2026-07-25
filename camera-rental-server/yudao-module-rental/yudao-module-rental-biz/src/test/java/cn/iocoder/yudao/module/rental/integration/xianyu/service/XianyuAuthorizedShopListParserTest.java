package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianyuAuthorizedShopListParserTest {

    private final XianyuAuthorizedShopListParser parser = new XianyuAuthorizedShopListParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseAuthorizedShopsFromPayload() throws Exception {
        String json = """
                {"code":0,"msg":"OK","data":{"list":[
                  {"authorize_id":922158952480837,"seller_id":922157703544901,
                   "shop_name":"票务大师","is_valid":true,"valid_end_time":1785081599,
                   "is_deposit_enough":true}
                ]}}
                """;
        List<XianyuAuthorizedShop> shops = parser.parse(objectMapper.readTree(json));
        assertEquals(1, shops.size());
        assertEquals("922158952480837", shops.get(0).authorizeId());
        assertEquals("922157703544901", shops.get(0).externalShopId());
        assertEquals("票务大师", shops.get(0).shopName());
        assertTrue(shops.get(0).valid());
        assertEquals(XianyuAuthorizedShopListParser.GUARANTEE_STATUS_HEALTHY, shops.get(0).guaranteeStatus());
    }

    @Test
    void shouldParseGuaranteeStatusConservatively() throws Exception {
        String json = """
                {"code":0,"msg":"OK","data":{"list":[
                  {"authorize_id":1,"seller_id":11,"shop_name":"ok","is_deposit_enough":"true"},
                  {"authorize_id":2,"seller_id":22,"shop_name":"bad","is_deposit_enough":false},
                  {"authorize_id":3,"seller_id":33,"shop_name":"unknown"}
                ]}}
                """;
        List<XianyuAuthorizedShop> shops = parser.parse(objectMapper.readTree(json));

        assertEquals(XianyuAuthorizedShopListParser.GUARANTEE_STATUS_HEALTHY, shops.get(0).guaranteeStatus());
        assertEquals(XianyuAuthorizedShopListParser.GUARANTEE_STATUS_DEPOSIT_INSUFFICIENT,
                shops.get(1).guaranteeStatus());
        assertEquals(XianyuAuthorizedShopListParser.GUARANTEE_STATUS_UNKNOWN, shops.get(2).guaranteeStatus());
    }

    @Test
    void shouldRejectMissingListInsteadOfTreatingMalformedPayloadAsEmptySnapshot() throws Exception {
        assertThrows(RuntimeException.class, () -> parser.parse(objectMapper.readTree("""
                {"code":0,"msg":"OK","data":{}}
                """)));
    }

}
