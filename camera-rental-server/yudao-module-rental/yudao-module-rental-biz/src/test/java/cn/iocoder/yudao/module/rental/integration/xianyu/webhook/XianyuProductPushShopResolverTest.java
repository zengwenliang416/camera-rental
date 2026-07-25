package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuProductPushShopResolverTest {

    private XianyuShopMapper shopMapper;
    private XianyuProductMapper productMapper;
    private XianyuProductPushShopResolver resolver;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        shopMapper = mock(XianyuShopMapper.class);
        productMapper = mock(XianyuProductMapper.class);
        resolver = new XianyuProductPushShopResolver(shopMapper, productMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldResolveUniqueSellerShopDirectly() {
        when(shopMapper.selectValidListByExternalShopId("seller-1")).thenReturn(List.of(XianyuShopDO.builder()
                .id(7L)
                .authorizationStatus("VALID")
                .build()));

        assertEquals(7L, resolver.resolveShopId("seller-1", "product-1"));
    }

    @Test
    void shouldUseCurrentTenantWhenFallingBackToPersistedProduct() {
        when(shopMapper.selectValidListByExternalShopId("seller-1")).thenReturn(List.of());
        when(productMapper.selectListByExternalProductId("product-1")).thenReturn(List.of(XianyuProductDO.builder()
                .shopId(8L)
                .externalProductId("product-1")
                .build()));
        when(shopMapper.selectByTenantIdAndId(9L, 8L)).thenReturn(XianyuShopDO.builder()
                .id(8L)
                .authorizationStatus("VALID")
                .build());

        assertEquals(8L, resolver.resolveShopId("seller-1", "product-1"));

        verify(shopMapper).selectByTenantIdAndId(9L, 8L);
    }

    @Test
    void shouldRejectFallbackShopWhenCurrentTenantCannotReadIt() {
        when(shopMapper.selectValidListByExternalShopId("seller-1")).thenReturn(List.of());
        when(productMapper.selectListByExternalProductId("product-1")).thenReturn(List.of(XianyuProductDO.builder()
                .shopId(8L)
                .externalProductId("product-1")
                .build()));

        assertNull(resolver.resolveShopId("seller-1", "product-1"));

        verify(shopMapper).selectByTenantIdAndId(9L, 8L);
    }

}
