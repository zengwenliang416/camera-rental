package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsCarrierMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsCarrierMappingMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderConfigMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderCredentialMapper;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.CarrierMappingCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderConfigCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderConfigView;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderCredentialCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.SecretAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RentalLogisticsOperationsConfigurationServiceTest {

    private final RentalLogisticsProviderConfigMapper configMapper =
            mock(RentalLogisticsProviderConfigMapper.class);
    private final RentalLogisticsProviderCredentialMapper credentialMapper =
            mock(RentalLogisticsProviderCredentialMapper.class);
    private final RentalLogisticsCarrierMappingMapper mappingMapper =
            mock(RentalLogisticsCarrierMappingMapper.class);
    private final RentalLogisticsConfigurationOperationsService service =
            new RentalLogisticsConfigurationOperationsService(configMapper, credentialMapper, mappingMapper);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void responseNeverReturnsPlaintextCredentials() {
        when(configMapper.selectByProviderCode(9L, "KUAIDI100"))
                .thenReturn(RentalLogisticsProviderConfigDO.builder()
                        .providerCode("KUAIDI100")
                        .callbackSecret("callback-secret")
                        .minimumQueryIntervalSeconds(1800)
                        .configStatus("LOCALLY_VERIFIED")
                        .build());
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100")).thenReturn(java.util.List.of(
                credential(11L, "primary", "customer-secret-1", "api-secret-1", true),
                credential(12L, "backup", "customer-secret-2", "api-secret-2", false)));

        ProviderConfigView view = service.getProviderConfig("kuaidi100");

        assertTrue(view.callbackSecretConfigured());
        assertEquals("********", view.maskedCallbackSecret());
        assertEquals(2, view.credentials().size());
        assertTrue(view.credentials().stream().allMatch(it -> it.customerCodeConfigured()
                && it.apiKeyConfigured()
                && "********".equals(it.maskedCustomerCode())
                && "********".equals(it.maskedApiKey())));
        assertFalse(view.toString().contains("customer-secret"));
        assertFalse(view.toString().contains("api-secret"));
    }

    @Test
    void newConfigDefaultsDisabledAndVerifyDoesNotEnableFeatures() {
        when(configMapper.selectByProviderCodeForUpdate(9L, "KUAIDI100")).thenReturn(null);
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100")).thenReturn(java.util.List.of());
        ProviderConfigCommand command = new ProviderConfigCommand("kuaidi100", null, null, null,
                SecretAction.REPLACE, "callback", "https://callback.example.com", 1800, "4");

        ProviderConfigView view = service.saveProviderConfig(command);

        assertFalse(view.enabled());
        assertFalse(view.queryEnabled());
        assertFalse(view.subscribeEnabled());
        ArgumentCaptor<RentalLogisticsProviderConfigDO> captor =
                ArgumentCaptor.forClass(RentalLogisticsProviderConfigDO.class);
        verify(configMapper).insert(captor.capture());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getEnabled()));
        assertEquals("INCOMPLETE", captor.getValue().getConfigStatus());
    }

    @Test
    void verifyPerformsOnlyLocalCompletenessValidation() {
        RentalLogisticsProviderConfigDO config = RentalLogisticsProviderConfigDO.builder()
                .providerCode("KUAIDI100")
                .enabled(false)
                .queryEnabled(false)
                .subscribeEnabled(false)
                .callbackSecret("callback")
                .callbackBaseUrl("https://callback.example.com")
                .build();
        when(configMapper.selectByProviderCodeForUpdate(9L, "KUAIDI100")).thenReturn(config);
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100"))
                .thenReturn(java.util.List.of(credential(11L, "primary", "customer", "key", true)));

        var result = service.verifyProviderConfig("KUAIDI100");

        assertTrue(result.valid());
        assertEquals("LOCAL_CONFIGURATION_VALID", result.reason());
        assertEquals("LOCALLY_VERIFIED", config.getConfigStatus());
        assertFalse(config.getEnabled());
        assertFalse(config.getQueryEnabled());
        assertFalse(config.getSubscribeEnabled());
        verify(configMapper).selectByProviderCodeForUpdate(9L, "KUAIDI100");
        verify(configMapper).updateById(config);
        verifyNoMoreInteractions(configMapper);
    }

    @Test
    void queryOnlyConfigurationNeedsCredentialsButNoCallbackSettings() {
        RentalLogisticsProviderConfigDO config = RentalLogisticsProviderConfigDO.builder()
                .providerCode("KUAIDI100")
                .enabled(false)
                .queryEnabled(true)
                .subscribeEnabled(false)
                .configStatus("INCOMPLETE")
                .build();
        when(configMapper.selectByProviderCode(9L, "KUAIDI100")).thenReturn(config);
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100"))
                .thenReturn(java.util.List.of(credential(11L, "primary", "customer", "key", true)));

        ProviderConfigView view = service.getProviderConfig("KUAIDI100");

        assertEquals("READY_UNVERIFIED", view.configStatus());
        assertFalse(view.callbackSecretConfigured());
        assertNull(view.callbackBaseUrl());
    }

    @Test
    void subscriptionConfigurationRequiresCallbackUrlButNotGlobalSecret() {
        RentalLogisticsProviderConfigDO config = RentalLogisticsProviderConfigDO.builder()
                .providerCode("KUAIDI100")
                .enabled(false)
                .queryEnabled(true)
                .subscribeEnabled(false)
                .configStatus("INCOMPLETE")
                .build();
        when(configMapper.selectByProviderCodeForUpdate(9L, "KUAIDI100")).thenReturn(config);
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100"))
                .thenReturn(java.util.List.of(credential(11L, "primary", "customer", "key", true)));

        ProviderConfigView view = service.saveProviderConfig(new ProviderConfigCommand(
                "KUAIDI100", true, true, true, SecretAction.KEEP, null,
                "https://api.example.com", 1800, "4"));

        assertEquals("READY_UNVERIFIED", view.configStatus());
        assertFalse(view.callbackSecretConfigured());
        assertEquals("https://api.example.com", view.callbackBaseUrl());
        verify(configMapper).updateById(config);
    }

    @Test
    void subscriptionConfigurationRejectsMissingCallbackUrl() {
        RentalLogisticsProviderConfigDO config = RentalLogisticsProviderConfigDO.builder()
                .providerCode("KUAIDI100")
                .enabled(false)
                .queryEnabled(true)
                .subscribeEnabled(false)
                .configStatus("INCOMPLETE")
                .build();
        when(configMapper.selectByProviderCodeForUpdate(9L, "KUAIDI100")).thenReturn(config);
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100"))
                .thenReturn(java.util.List.of(credential(11L, "primary", "customer", "key", true)));

        RentalLogisticsException exception = assertThrows(RentalLogisticsException.class,
                () -> service.saveProviderConfig(new ProviderConfigCommand(
                        "KUAIDI100", true, true, true, SecretAction.KEEP, null,
                        null, 1800, "4")));

        assertEquals("PROVIDER_CONFIG_INCOMPLETE", exception.getCode());
        verify(configMapper, never()).updateById(config);
    }

    @Test
    void savesMultipleCredentialsWithoutReturningPlaintext() {
        when(credentialMapper.selectByNameForUpdate(9L, "KUAIDI100", "primary")).thenReturn(null);
        when(configMapper.selectByProviderCodeForUpdate(9L, "KUAIDI100")).thenReturn(null);

        var view = service.saveProviderCredential(new ProviderCredentialCommand(null, "kuaidi100",
                "primary", true, 10, SecretAction.REPLACE, "customer-secret",
                SecretAction.REPLACE, "api-secret"));

        ArgumentCaptor<RentalLogisticsProviderCredentialDO> captor =
                ArgumentCaptor.forClass(RentalLogisticsProviderCredentialDO.class);
        verify(credentialMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getTenantId());
        assertEquals("KUAIDI100", captor.getValue().getProviderCode());
        assertEquals("primary", captor.getValue().getCredentialName());
        assertEquals("customer-secret", captor.getValue().getCustomerCode());
        assertEquals("api-secret", captor.getValue().getApiKey());
        assertEquals("********", view.maskedCustomerCode());
        assertEquals("********", view.maskedApiKey());
        assertFalse(view.toString().contains("customer-secret"));
        assertFalse(view.toString().contains("api-secret"));
    }

    @Test
    void updateCredentialRejectsIdOutsideCurrentTenant() {
        when(credentialMapper.selectByTenantIdAndIdForUpdate(9L, 88L)).thenReturn(null);

        RentalLogisticsException exception = assertThrows(RentalLogisticsException.class,
                () -> service.saveProviderCredential(new ProviderCredentialCommand(88L, "KUAIDI100",
                        "foreign", true, 20, SecretAction.KEEP, null, SecretAction.KEEP, null)));

        assertEquals("PROVIDER_CREDENTIAL_NOT_FOUND", exception.getCode());
        verify(credentialMapper, never()).insert(any(RentalLogisticsProviderCredentialDO.class));
        verify(credentialMapper, never()).updateById(any(RentalLogisticsProviderCredentialDO.class));
    }

    @Test
    void enablingProviderRequiresAtLeastOneCompleteEnabledCredential() {
        RentalLogisticsProviderConfigDO config = RentalLogisticsProviderConfigDO.builder()
                .providerCode("KUAIDI100")
                .enabled(false)
                .callbackSecret("callback")
                .callbackBaseUrl("https://callback.example.com")
                .build();
        when(configMapper.selectByProviderCodeForUpdate(9L, "KUAIDI100")).thenReturn(config);
        when(credentialMapper.selectListByProvider(9L, "KUAIDI100"))
                .thenReturn(java.util.List.of(credential(11L, "disabled", "customer", "key", false)));

        RentalLogisticsException exception = assertThrows(RentalLogisticsException.class,
                () -> service.saveProviderConfig(new ProviderConfigCommand("KUAIDI100", true, true,
                        true, SecretAction.KEEP, null, null, null, null)));

        assertEquals("PROVIDER_CONFIG_INCOMPLETE", exception.getCode());
        verify(configMapper, never()).updateById(config);
    }

    @Test
    void mappingWriteIsTenantScopedNormalizedAndDisabledByDefault() {
        when(mappingMapper.selectBySourceForUpdate(9L, "XIANYU", "SF")).thenReturn(null);
        CarrierMappingCommand command = new CarrierMappingCommand(null, " xianyu ", " sf ", " sf ",
                "顺丰速运", " kuaidi100 ", " shunfeng ", null, null);

        service.saveCarrierMapping(command);

        ArgumentCaptor<RentalLogisticsCarrierMappingDO> captor =
                ArgumentCaptor.forClass(RentalLogisticsCarrierMappingDO.class);
        verify(mappingMapper).insert(captor.capture());
        RentalLogisticsCarrierMappingDO saved = captor.getValue();
        assertEquals(9L, saved.getTenantId());
        assertEquals("XIANYU", saved.getSourceType());
        assertEquals("SF", saved.getSourceCarrierCode());
        assertEquals("KUAIDI100", saved.getProviderCode());
        assertEquals("DISABLED", saved.getStatus());
        assertEquals("OPTIONAL", saved.getPhoneRequirement());
    }

    @Test
    void callbackBaseUrlRejectsEmbeddedCredentialsAndQuerySecrets() {
        when(configMapper.selectByProviderCodeForUpdate(9L, "KUAIDI100")).thenReturn(null);
        ProviderConfigCommand command = new ProviderConfigCommand("KUAIDI100", null, null, null,
                SecretAction.KEEP, null, "https://user:secret@example.com/callback?token=secret",
                1800, "4");

        RentalLogisticsException exception = assertThrows(RentalLogisticsException.class,
                () -> service.saveProviderConfig(command));

        assertEquals("CALLBACK_BASE_URL_INVALID", exception.getCode());
        verify(configMapper, never()).insert(any(RentalLogisticsProviderConfigDO.class));
    }

    private RentalLogisticsProviderCredentialDO credential(Long id, String name, String customerCode,
                                                           String apiKey, boolean enabled) {
        RentalLogisticsProviderCredentialDO credential = RentalLogisticsProviderCredentialDO.builder()
                .id(id)
                .providerCode("KUAIDI100")
                .credentialName(name)
                .enabled(enabled)
                .sortOrder(100)
                .customerCode(customerCode)
                .apiKey(apiKey)
                .configStatus("LOCALLY_VERIFIED")
                .build();
        credential.setTenantId(9L);
        return credential;
    }
}
