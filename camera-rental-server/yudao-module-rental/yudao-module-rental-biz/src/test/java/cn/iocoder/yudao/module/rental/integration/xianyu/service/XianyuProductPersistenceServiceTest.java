package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuProductPersistenceServiceTest {

    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuProductMapper productMapper;

    private XianyuProductPersistenceService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        service = new XianyuProductPersistenceService(
                new XianyuProductDetailPayloadParser(new ObjectMapper()),
                new XianyuPayloadHasher(),
                rawPayloadMapper,
                productMapper,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPersistRawBeforeNormalizedProduct() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        when(productMapper.selectByShopIdAndExternalProductIdForUpdate(77L, "441160510721413"))
                .thenReturn(null);

        service.persistProductDetail(77L, response(1694000092));

        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        assertEquals("PRODUCT_DETAIL", rawCaptor.getValue().getSourceType());
        assertEquals("RESTRICTED_UNREDACTED_V1", rawCaptor.getValue().getRedactionVersion());
        ArgumentCaptor<XianyuProductDO> productCaptor = ArgumentCaptor.forClass(XianyuProductDO.class);
        verify(productMapper).insert(productCaptor.capture());
        assertEquals(77L, productCaptor.getValue().getShopId());
        assertEquals("441160510721413", productCaptor.getValue().getExternalProductId());
        assertEquals("Sony A7M4", productCaptor.getValue().getTitle());
        assertEquals("22", productCaptor.getValue().getStatus());
        assertEquals(31L, productCaptor.getValue().getRawPayloadId());
    }

    @Test
    void shouldNotOverwriteNewerStoredSnapshot() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        XianyuProductDO existing = XianyuProductDO.builder()
                .id(41L)
                .sourceUpdatedAt(LocalDateTime.of(2023, 9, 7, 0, 0))
                .build();
        when(productMapper.selectByShopIdAndExternalProductIdForUpdate(77L, "441160510721413"))
                .thenReturn(existing);

        XianyuProductDO result = service.persistProductDetail(77L, response(1694000092));

        assertEquals(existing, result);
        verify(productMapper, never()).updateById(any(XianyuProductDO.class));
    }

    private String response(long updateTime) {
        return """
                {"code":0,"msg":"OK","data":{"product_id":441160510721413,
                "product_status":22,"channel_cat_id":"camera","title":"Sony A7M4",
                "update_time":%d}}
                """.formatted(updateTime);
    }

}
