package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RentalLogisticsProviderConfigServiceTest {

    private final RentalLogisticsProviderConfigMapper mapper = mock(RentalLogisticsProviderConfigMapper.class);
    private final RentalLogisticsProviderConfigService service = new RentalLogisticsProviderConfigService(mapper);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void neverAllowsQueryIntervalBelowThirtyMinutes() {
        when(mapper.selectByProviderCode(9L, "KUAIDI100"))
                .thenReturn(RentalLogisticsProviderConfigDO.builder()
                        .minimumQueryIntervalSeconds(60)
                        .build());

        assertEquals(1800, service.minimumQueryIntervalSeconds("KUAIDI100"));
    }
}
