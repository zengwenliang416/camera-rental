package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class XianyuOrderPushShopResolverTest {

    private final XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
    private final XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
    private final XianyuOrderPushShopResolver resolver = new XianyuOrderPushShopResolver(shopMapper, orderMapper);

    @Test
    void shouldUseOnlyAuthorizedShopWithoutOrderLookup() {
        when(shopMapper.selectValidListByExternalShopId("seller-1"))
                .thenReturn(List.of(XianyuShopDO.builder().id(7L).build()));

        assertEquals(7L, resolver.resolveShopId("seller-1", "order-1"));
        verifyNoInteractions(orderMapper);
    }

    @Test
    void shouldResolveSharedSellerFromExistingOrder() {
        List<XianyuShopDO> shops = List.of(
                XianyuShopDO.builder().id(7L).build(),
                XianyuShopDO.builder().id(8L).build());
        when(shopMapper.selectValidListByExternalShopId("seller-1")).thenReturn(shops);
        when(orderMapper.selectListByShopIdsAndExternalOrderId(List.of(7L, 8L), "order-1"))
                .thenReturn(List.of(XianyuOrderDO.builder().shopId(8L).build()));

        assertEquals(8L, resolver.resolveShopId("seller-1", "order-1"));
    }

    @Test
    void shouldNotGuessWhenExistingOrderIsStillAmbiguous() {
        List<XianyuShopDO> shops = List.of(
                XianyuShopDO.builder().id(7L).build(),
                XianyuShopDO.builder().id(8L).build());
        when(shopMapper.selectValidListByExternalShopId("seller-1")).thenReturn(shops);
        when(orderMapper.selectListByShopIdsAndExternalOrderId(List.of(7L, 8L), "order-1"))
                .thenReturn(List.of(
                        XianyuOrderDO.builder().shopId(7L).build(),
                        XianyuOrderDO.builder().shopId(8L).build()));

        assertNull(resolver.resolveShopId("seller-1", "order-1"));
    }

}
