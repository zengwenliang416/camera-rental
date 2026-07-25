package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductSkuMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuProductSkuPersistenceServiceTest {

    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuProductMapper productMapper;
    @Mock
    private XianyuProductSkuMapper productSkuMapper;

    private XianyuProductSkuPersistenceService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        service = new XianyuProductSkuPersistenceService(
                new XianyuProductSkuPayloadParser(new ObjectMapper()),
                new XianyuPayloadHasher(),
                rawPayloadMapper,
                productMapper,
                productSkuMapper,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPersistRawBeforeNormalizedSkus() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        when(productMapper.selectByShopIdAndExternalProductId(77L, "537044127563781"))
                .thenReturn(XianyuProductDO.builder().id(51L).build());
        when(productSkuMapper.selectByProductIdAndExternalSkuIdForUpdate(51L, "537044127563786"))
                .thenReturn(null);

        int count = service.persistProductSkus(77L, response());

        assertEquals(1, count);
        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        assertEquals("PRODUCT_SKUS", rawCaptor.getValue().getSourceType());
        ArgumentCaptor<XianyuProductSkuDO> skuCaptor = ArgumentCaptor.forClass(XianyuProductSkuDO.class);
        verify(productSkuMapper).insert(skuCaptor.capture());
        assertEquals(51L, skuCaptor.getValue().getProductId());
        assertEquals("537044127563786", skuCaptor.getValue().getExternalSkuId());
        assertEquals("颜色:蓝色", skuCaptor.getValue().getSkuName());
        assertEquals(1, skuCaptor.getValue().getSourceStock());
        assertEquals(31L, skuCaptor.getValue().getRawPayloadId());
    }

    private String response() {
        return """
                {"code":0,"msg":"OK","data":{"list":[{"product_id":537044127563781,
                "sku_items":[{"sku_id":537044127563786,"price":2,"stock":1,
                "sku_text":"颜色:蓝色","outer_id":"gyfbcs240416001"}]}]}}
                """;
    }

}
