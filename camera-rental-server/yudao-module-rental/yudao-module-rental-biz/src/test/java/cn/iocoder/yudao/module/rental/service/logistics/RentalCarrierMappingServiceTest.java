package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsCarrierMappingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RentalCarrierMappingServiceTest {

    private final RentalLogisticsCarrierMappingMapper mapper = mock(RentalLogisticsCarrierMappingMapper.class);
    private final RentalCarrierMappingService service = new RentalCarrierMappingService(mapper);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void normalizesLookupAndFallbackCarrierCodesInOnePlace() {
        RentalCarrierResolution resolution = service.resolve(" xianyu ", " sf ");

        assertEquals("XIANYU", resolution.sourceType());
        assertEquals("SF", resolution.sourceCarrierCode());
        assertEquals("SF", resolution.canonicalCarrierCode());
        assertNull(resolution.mapping());
        verify(mapper).selectEnabled(9L, "XIANYU", "SF");
    }
}
