package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderCredentialMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalLogisticsProviderCredentialServiceTest {

    private final RentalLogisticsProviderCredentialMapper credentialMapper =
            mock(RentalLogisticsProviderCredentialMapper.class);
    private final RentalLogisticsProviderCredentialService service =
            new RentalLogisticsProviderCredentialService(credentialMapper);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void reusesUsableCredentialAlreadyBoundToDelivery() {
        RentalLogisticsProviderCredentialDO bound = credential(12L, true, "customer", "key");
        when(credentialMapper.selectByTenantIdAndId(9L, 12L)).thenReturn(bound);
        RentalDeliveryDO delivery = delivery(100L, 12L);

        RentalLogisticsProviderCredentialDO resolved = service.resolveForDelivery(delivery);

        assertEquals(12L, resolved.getId());
        verify(credentialMapper, never()).selectListByProvider(9L, "KUAIDI100");
    }

    @Test
    void distributesUnboundDeliveriesAcrossOrderedCredentialPool() {
        List<RentalLogisticsProviderCredentialDO> pool = List.of(
                credential(11L, true, "customer-1", "key-1"),
                credential(12L, true, "customer-2", "key-2"));
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100")).thenReturn(pool);

        RentalLogisticsProviderCredentialDO first = service.resolveForDelivery(delivery(100L, null));
        RentalLogisticsProviderCredentialDO second = service.resolveForDelivery(delivery(101L, null));

        assertEquals(11L, first.getId());
        assertEquals(12L, second.getId());
    }

    @Test
    void reselectsWhenBoundCredentialIsDisabled() {
        when(credentialMapper.selectByTenantIdAndId(9L, 12L))
                .thenReturn(credential(12L, false, "customer-old", "key-old"));
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100"))
                .thenReturn(List.of(credential(13L, true, "customer-new", "key-new")));

        RentalLogisticsProviderCredentialDO resolved =
                service.resolveForDelivery(delivery(100L, 12L));

        assertEquals(13L, resolved.getId());
    }

    @Test
    void ignoresIncompleteAndCrossTenantCredentials() {
        when(credentialMapper.selectByTenantIdAndId(9L, 99L)).thenReturn(null);
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100")).thenReturn(List.of(
                credential(11L, true, null, "key"),
                credential(12L, true, "customer", null),
                credential(13L, false, "customer", "key")));

        assertNull(service.resolveForDelivery(delivery(100L, 99L)));
        assertFalse(service.hasUsableCredential("KUAIDI100"));
        verify(credentialMapper).selectByTenantIdAndId(9L, 99L);
    }

    private RentalDeliveryDO delivery(Long id, Long credentialId) {
        return RentalDeliveryDO.builder()
                .id(id)
                .providerCode("KUAIDI100")
                .providerCredentialId(credentialId)
                .build();
    }

    private RentalLogisticsProviderCredentialDO credential(Long id, boolean enabled,
                                                           String customerCode, String apiKey) {
        RentalLogisticsProviderCredentialDO credential = RentalLogisticsProviderCredentialDO.builder()
                .id(id)
                .providerCode("KUAIDI100")
                .credentialName("credential-" + id)
                .enabled(enabled)
                .sortOrder(id.intValue())
                .customerCode(customerCode)
                .apiKey(apiKey)
                .build();
        credential.setTenantId(9L);
        return credential;
    }
}
