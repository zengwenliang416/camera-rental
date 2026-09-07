package cn.iocoder.yudao.module.rental.service.rental;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100AddressResolutionClient;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderConfigService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderCredentialService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RentalAddressParseServiceTest {

    private final RentalLogisticsProviderConfigService configService =
            mock(RentalLogisticsProviderConfigService.class);
    private final RentalLogisticsProviderCredentialService credentialService =
            mock(RentalLogisticsProviderCredentialService.class);
    private final Kuaidi100AddressResolutionClient client = mock(Kuaidi100AddressResolutionClient.class);
    private final RentalAddressParseService service =
            new RentalAddressParseService(configService, credentialService, client);

    @Test
    void localFallbackExtractsUniqueMobileAndDeduplicatesMunicipality() {
        RentalAddressParseResult result = service.parse(
                "Nicole，13800000000，上海上海市松江区永丰街道秀庭酒店(上海松江站店)");

        assertEquals("Nicole", result.name());
        assertEquals("13800000000", result.mobile());
        assertEquals("上海市松江区永丰街道秀庭酒店(上海松江站店)", result.address());
        assertEquals("LOCAL", result.source());
        assertTrue(result.fallback());
        assertTrue(result.warnings().contains("PROVIDER_DISABLED_LOCAL_FALLBACK"));
    }

    @Test
    void multipleMobilesAreNotChosenAutomatically() {
        RentalAddressParseResult result = service.parseLocally(
                "张三，13800000000，备用13900000000，上海市松江区人民路1号");

        assertNull(result.mobile());
        assertTrue(result.warnings().contains("MULTIPLE_MOBILES_REVIEW"));
    }

    @Test
    void providerFailureFallsBackWithoutLeakingException() throws Exception {
        RentalLogisticsProviderCredentialDO credential = RentalLogisticsProviderCredentialDO.builder()
                .apiKey("key").apiSecret("secret").build();
        when(configService.isAddressParseEnabled("KUAIDI100")).thenReturn(true);
        when(credentialService.resolveForAddressParse("KUAIDI100")).thenReturn(credential);
        when(client.resolve("李四，13800000000，上海市松江区人民路1号", credential))
                .thenThrow(new IOException("remote payload contains private data"));

        RentalAddressParseResult result = service.parse("李四，13800000000，上海市松江区人民路1号");

        assertEquals("LOCAL", result.source());
        assertTrue(result.warnings().contains("PROVIDER_UNAVAILABLE_LOCAL_FALLBACK"));
    }
}
