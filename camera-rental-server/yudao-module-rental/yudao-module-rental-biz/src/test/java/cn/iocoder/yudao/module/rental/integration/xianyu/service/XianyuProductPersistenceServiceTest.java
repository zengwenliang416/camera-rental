package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationTrigger;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuProductPersistenceServiceTest {

    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuProductMapper productMapper;
    @Mock
    private XianyuShopMapper shopMapper;
    @Mock
    private RentalChannelOrderReconciliationTrigger reconciliationTrigger;

    private XianyuProductPersistenceService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        service = new XianyuProductPersistenceService(
                new XianyuProductDetailPayloadParser(
                        new ObjectMapper(), new XianyuChannelIdentifierNormalizer()),
                new XianyuPayloadHasher(),
                rawPayloadMapper,
                productMapper,
                shopMapper,
                reconciliationTrigger,
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
        when(shopMapper.selectByTenantIdAndId(9L, 77L))
                .thenReturn(XianyuShopDO.builder().id(77L).xianyuUserName("shop-user-1").build());
        when(productMapper.selectByShopIdAndXgjProductIdForUpdate(77L, "441160510721413"))
                .thenReturn(null);

        service.persistProductDetail(77L, response(1694000092));

        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        assertEquals("PRODUCT_DETAIL", rawCaptor.getValue().getSourceType());
        assertEquals("RESTRICTED_UNREDACTED_V1", rawCaptor.getValue().getRedactionVersion());
        ArgumentCaptor<XianyuProductDO> productCaptor = ArgumentCaptor.forClass(XianyuProductDO.class);
        verify(productMapper).insert(productCaptor.capture());
        assertEquals(77L, productCaptor.getValue().getShopId());
        assertEquals("441160510721413", productCaptor.getValue().getXgjProductId());
        assertEquals("1062409679830", productCaptor.getValue().getXianyuItemId());
        assertNull(productCaptor.getValue().getExternalProductId());
        assertEquals("Sony A7M4", productCaptor.getValue().getTitle());
        assertEquals("22", productCaptor.getValue().getStatus());
        assertEquals(31L, productCaptor.getValue().getRawPayloadId());
        verify(reconciliationTrigger).afterProductChange(77L, "441160510721413");
    }

    @Test
    void shouldNotOverwriteNewerStoredSnapshot() {
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        when(shopMapper.selectByTenantIdAndId(9L, 77L))
                .thenReturn(XianyuShopDO.builder().id(77L).xianyuUserName("shop-user-1").build());
        XianyuProductDO existing = XianyuProductDO.builder()
                .id(41L)
                .sourceUpdatedAt(LocalDateTime.of(2023, 9, 7, 0, 0))
                .build();
        when(productMapper.selectByShopIdAndXgjProductIdForUpdate(77L, "441160510721413"))
                .thenReturn(existing);

        XianyuProductDO result = service.persistProductDetail(77L, response(1694000092));

        assertEquals(existing, result);
        verify(productMapper, never()).updateById(any(XianyuProductDO.class));
        verifyNoInteractions(reconciliationTrigger);
    }

    @Test
    void shouldMatchOnlyTheExactShopUserName() {
        when(shopMapper.selectByTenantIdAndId(9L, 77L))
                .thenReturn(XianyuShopDO.builder().id(77L).xianyuUserName("shop-user-1").build());
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        when(productMapper.selectByShopIdAndXgjProductIdForUpdate(77L, "441160510721413"))
                .thenReturn(null);

        service.persistProductDetail(77L, responseWithPublishShops("""
                {"user_name":"shop-user","item_id":1,"status":3},
                {"user_name":"shop-user-1","item_id":1062409679830,"status":3}
                """));

        ArgumentCaptor<XianyuProductDO> captor = ArgumentCaptor.forClass(XianyuProductDO.class);
        verify(productMapper).insert(captor.capture());
        assertEquals("1062409679830", captor.getValue().getXianyuItemId());
    }

    @Test
    void shouldRejectMissingOrAmbiguousOwnedPublishItemBeforeWriting() {
        when(shopMapper.selectByTenantIdAndId(9L, 77L))
                .thenReturn(XianyuShopDO.builder().id(77L).xianyuUserName("shop-user-1").build());

        assertThrows(IllegalStateException.class, () -> service.persistProductDetail(
                77L, responseWithPublishShops("""
                        {"user_name":"another-shop","item_id":1,"status":3}
                        """)));
        assertThrows(IllegalStateException.class, () -> service.persistProductDetail(
                77L, responseWithPublishShops("""
                        {"user_name":"shop-user-1","item_id":1,"status":3},
                        {"user_name":"shop-user-1","item_id":2,"status":3}
                        """)));

        verifyNoInteractions(rawPayloadMapper, productMapper);
    }

    @Test
    void shouldPersistNullItemIdWhenTheExactShopEntryHasNoItemId() {
        when(shopMapper.selectByTenantIdAndId(9L, 77L))
                .thenReturn(XianyuShopDO.builder().id(77L).xianyuUserName("shop-user-1").build());
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        when(productMapper.selectByShopIdAndXgjProductIdForUpdate(77L, "441160510721413"))
                .thenReturn(null);

        service.persistProductDetail(77L, responseWithPublishShops("""
                {"user_name":"shop-user-1","status":3}
                """));

        ArgumentCaptor<XianyuProductDO> captor = ArgumentCaptor.forClass(XianyuProductDO.class);
        verify(productMapper).insert(captor.capture());
        assertNull(captor.getValue().getXianyuItemId());
    }

    private String response(long updateTime) {
        return responseWithPublishShops("""
                {"user_name":"shop-user-1","item_id":1062409679830,"status":3}
                """, updateTime);
    }

    private String responseWithPublishShops(String publishShops) {
        return responseWithPublishShops(publishShops, 1694000092);
    }

    private String responseWithPublishShops(String publishShops, long updateTime) {
        return """
                {"code":0,"msg":"OK","data":{"product_id":441160510721413,
                "product_status":22,"channel_cat_id":"camera","title":"Sony A7M4",
                "publish_shop":[%s],
                "update_time":%d}}
                """.formatted(publishShops, updateTime);
    }

}
