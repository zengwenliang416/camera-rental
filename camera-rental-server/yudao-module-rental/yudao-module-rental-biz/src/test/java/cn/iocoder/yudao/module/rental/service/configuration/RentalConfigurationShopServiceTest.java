package cn.iocoder.yudao.module.rental.service.configuration;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalConfigurationShopRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalConfigurationShopServiceTest {

    @Mock
    private XianyuShopMapper shopMapper;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void returnsOnlyCurrentTenantValidUnexpiredShops() {
        TenantContextHolder.setTenantId(9L);
        when(shopMapper.selectConfigurationShops(9L)).thenReturn(List.of(
                shop(1L, "可用店铺", "VALID", null),
                shop(2L, "未来到期", "VALID", LocalDateTime.now().plusDays(1)),
                shop(3L, "已过期", "VALID", LocalDateTime.now().minusDays(1)),
                shop(4L, "无效授权", "INVALID", null)));
        RentalConfigurationShopService service = new RentalConfigurationShopService(shopMapper);

        List<RentalConfigurationShopRespVO> result = service.getAvailableShops();

        assertEquals(
                List.of(1L, 2L),
                result.stream().map(RentalConfigurationShopRespVO::getId).toList());
        verify(shopMapper).selectConfigurationShops(9L);
    }

    private static XianyuShopDO shop(
            Long id, String name, String status, LocalDateTime expiresAt) {
        XianyuShopDO shop = new XianyuShopDO();
        shop.setId(id);
        shop.setShopName(name);
        shop.setAuthorizationStatus(status);
        shop.setAuthorizationExpiresAt(expiresAt);
        return shop;
    }

}
