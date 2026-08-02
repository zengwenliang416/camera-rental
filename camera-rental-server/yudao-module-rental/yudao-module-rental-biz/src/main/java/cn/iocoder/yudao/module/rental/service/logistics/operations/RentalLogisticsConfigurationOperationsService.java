package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsCarrierMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsCarrierMappingMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderConfigMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderCredentialMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalCarrierMappingStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalLogisticsPhoneRequirementEnum;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.*;

@Service
public class RentalLogisticsConfigurationOperationsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MAXIMUM_QUERY_INTERVAL_SECONDS = 86400;
    private static final String MASKED = "********";

    private final RentalLogisticsProviderConfigMapper configMapper;
    private final RentalLogisticsProviderCredentialMapper credentialMapper;
    private final RentalLogisticsCarrierMappingMapper mappingMapper;

    public RentalLogisticsConfigurationOperationsService(RentalLogisticsProviderConfigMapper configMapper,
                                                         RentalLogisticsProviderCredentialMapper credentialMapper,
                                                         RentalLogisticsCarrierMappingMapper mappingMapper) {
        this.configMapper = configMapper;
        this.credentialMapper = credentialMapper;
        this.mappingMapper = mappingMapper;
    }

    public ProviderConfigView getProviderConfig(String providerCode) {
        String normalizedProviderCode = normalizeRequired(providerCode, "PROVIDER_CODE_REQUIRED");
        RentalLogisticsProviderConfigDO config = configMapper.selectByProviderCode(
                TenantContextHolder.getRequiredTenantId(), normalizedProviderCode);
        return toView(config == null ? defaultConfig(normalizedProviderCode) : config,
                credentialMapper.selectListByProvider(
                        TenantContextHolder.getRequiredTenantId(), normalizedProviderCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderConfigView saveProviderConfig(ProviderConfigCommand command) {
        if (command == null) {
            throw new RentalLogisticsException("PROVIDER_CONFIG_REQUIRED");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String providerCode = normalizeRequired(command.providerCode(), "PROVIDER_CODE_REQUIRED");
        RentalLogisticsProviderConfigDO config = configMapper.selectByProviderCodeForUpdate(tenantId, providerCode);
        boolean created = config == null;
        if (created) {
            config = defaultConfig(providerCode);
            config.setTenantId(tenantId);
        }
        applyBoolean(command.enabled(), config::setEnabled);
        applyBoolean(command.queryEnabled(), config::setQueryEnabled);
        applyBoolean(command.subscribeEnabled(), config::setSubscribeEnabled);
        config.setCallbackSecret(applySecret(config.getCallbackSecret(), command.callbackSecretAction(),
                command.callbackSecret(), "CALLBACK_SECRET_REQUIRED"));
        if (command.callbackBaseUrl() != null) {
            config.setCallbackBaseUrl(validateCallbackBaseUrl(command.callbackBaseUrl()));
        }
        if (command.minimumQueryIntervalSeconds() != null) {
            int interval = command.minimumQueryIntervalSeconds();
            if (interval < RentalLogisticsProviderConfigService.MINIMUM_QUERY_INTERVAL_SECONDS
                    || interval > MAXIMUM_QUERY_INTERVAL_SECONDS) {
                throw new RentalLogisticsException("QUERY_INTERVAL_OUT_OF_RANGE");
            }
            config.setMinimumQueryIntervalSeconds(interval);
        }
        if (command.resultVersion() != null) {
            config.setResultVersion(normalizeRequired(command.resultVersion(), "RESULT_VERSION_REQUIRED"));
        }
        List<RentalLogisticsProviderCredentialDO> credentials =
                credentialMapper.selectListByProvider(tenantId, providerCode);
        config.setConfigStatus(isProviderReady(config, credentials) ? "READY_UNVERIFIED" : "INCOMPLETE");
        if (Boolean.TRUE.equals(config.getEnabled()) && !isProviderReady(config, credentials)) {
            throw new RentalLogisticsException("PROVIDER_CONFIG_INCOMPLETE");
        }
        if (created) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return toView(config, credentials);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderVerifyResult verifyProviderConfig(String providerCode) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String normalizedProviderCode = normalizeRequired(providerCode, "PROVIDER_CODE_REQUIRED");
        RentalLogisticsProviderConfigDO config =
                configMapper.selectByProviderCodeForUpdate(tenantId, normalizedProviderCode);
        List<RentalLogisticsProviderCredentialDO> credentials =
                credentialMapper.selectListByProvider(tenantId, normalizedProviderCode);
        if (config == null || !isProviderReady(config, credentials)) {
            if (config != null) {
                config.setConfigStatus("INCOMPLETE");
                configMapper.updateById(config);
            }
            return new ProviderVerifyResult(false, "CONFIG_INCOMPLETE", null);
        }
        LocalDateTime verifiedAt = LocalDateTime.now(BUSINESS_ZONE);
        config.setConfigStatus("LOCALLY_VERIFIED");
        config.setLastVerifiedAt(verifiedAt);
        configMapper.updateById(config);
        return new ProviderVerifyResult(true, "LOCAL_CONFIGURATION_VALID", verifiedAt);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderCredentialView saveProviderCredential(ProviderCredentialCommand command) {
        if (command == null) {
            throw new RentalLogisticsException("PROVIDER_CREDENTIAL_REQUIRED");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String providerCode = normalizeRequired(command.providerCode(), "PROVIDER_CODE_REQUIRED");
        String credentialName = requireText(command.credentialName(), "CREDENTIAL_NAME_REQUIRED");
        RentalLogisticsProviderCredentialDO credential = command.id() == null
                ? credentialMapper.selectByNameForUpdate(tenantId, providerCode, credentialName)
                : credentialMapper.selectByTenantIdAndIdForUpdate(tenantId, command.id());
        if (command.id() != null && credential == null) {
            throw new RentalLogisticsException("PROVIDER_CREDENTIAL_NOT_FOUND");
        }
        if (credential != null && !providerCode.equals(credential.getProviderCode())) {
            throw new RentalLogisticsException("PROVIDER_CREDENTIAL_NOT_FOUND");
        }
        boolean created = credential == null;
        if (created) {
            credential = RentalLogisticsProviderCredentialDO.builder()
                    .providerCode(providerCode)
                    .enabled(false)
                    .sortOrder(100)
                    .configStatus("INCOMPLETE")
                    .build();
            credential.setTenantId(tenantId);
        }
        credential.setCredentialName(credentialName);
        applyBoolean(command.enabled(), credential::setEnabled);
        if (command.sortOrder() != null) {
            if (command.sortOrder() < 0 || command.sortOrder() > 10000) {
                throw new RentalLogisticsException("CREDENTIAL_SORT_ORDER_OUT_OF_RANGE");
            }
            credential.setSortOrder(command.sortOrder());
        }
        credential.setCustomerCode(applySecret(credential.getCustomerCode(), command.customerCodeAction(),
                command.customerCode(), "CUSTOMER_CODE_REQUIRED"));
        credential.setApiKey(applySecret(credential.getApiKey(), command.apiKeyAction(),
                command.apiKey(), "API_KEY_REQUIRED"));
        credential.setConfigStatus(isCredentialComplete(credential) ? "READY_UNVERIFIED" : "INCOMPLETE");
        if (Boolean.TRUE.equals(credential.getEnabled()) && !isCredentialComplete(credential)) {
            throw new RentalLogisticsException("PROVIDER_CREDENTIAL_INCOMPLETE");
        }
        if (created) {
            credentialMapper.insert(credential);
        } else {
            credentialMapper.updateById(credential);
        }
        refreshProviderStatus(tenantId, providerCode);
        return toView(credential);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProviderCredential(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalLogisticsProviderCredentialDO credential =
                credentialMapper.selectByTenantIdAndIdForUpdate(tenantId, id);
        if (credential == null) {
            throw new RentalLogisticsException("PROVIDER_CREDENTIAL_NOT_FOUND");
        }
        credentialMapper.deleteById(id);
        refreshProviderStatus(tenantId, credential.getProviderCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderVerifyResult verifyProviderCredential(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalLogisticsProviderCredentialDO credential =
                credentialMapper.selectByTenantIdAndIdForUpdate(tenantId, id);
        if (credential == null) {
            throw new RentalLogisticsException("PROVIDER_CREDENTIAL_NOT_FOUND");
        }
        if (!isCredentialComplete(credential)) {
            credential.setConfigStatus("INCOMPLETE");
            credentialMapper.updateById(credential);
            refreshProviderStatus(tenantId, credential.getProviderCode());
            return new ProviderVerifyResult(false, "CONFIG_INCOMPLETE", null);
        }
        LocalDateTime verifiedAt = LocalDateTime.now(BUSINESS_ZONE);
        credential.setConfigStatus("LOCALLY_VERIFIED");
        credential.setLastVerifiedAt(verifiedAt);
        credentialMapper.updateById(credential);
        refreshProviderStatus(tenantId, credential.getProviderCode());
        return new ProviderVerifyResult(true, "LOCAL_CONFIGURATION_VALID", verifiedAt);
    }

    public List<CarrierMappingView> listCarrierMappings() {
        return mappingMapper.selectListByTenant(TenantContextHolder.getRequiredTenantId())
                .stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CarrierMappingView saveCarrierMapping(CarrierMappingCommand command) {
        if (command == null) {
            throw new RentalLogisticsException("CARRIER_MAPPING_REQUIRED");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String sourceType = normalizeRequired(command.sourceType(), "SOURCE_TYPE_REQUIRED");
        String sourceCarrierCode = normalizeRequired(command.sourceCarrierCode(), "SOURCE_CARRIER_CODE_REQUIRED");
        RentalLogisticsCarrierMappingDO mapping =
                mappingMapper.selectBySourceForUpdate(tenantId, sourceType, sourceCarrierCode);
        if (command.id() != null && mapping != null && !command.id().equals(mapping.getId())) {
            throw new RentalLogisticsException("CARRIER_MAPPING_ID_CONFLICT");
        }
        boolean created = mapping == null;
        if (created) {
            mapping = new RentalLogisticsCarrierMappingDO();
            mapping.setTenantId(tenantId);
            mapping.setSourceType(sourceType);
            mapping.setSourceCarrierCode(sourceCarrierCode);
        }
        mapping.setCanonicalCarrierCode(
                normalizeRequired(command.canonicalCarrierCode(), "CANONICAL_CARRIER_CODE_REQUIRED"));
        mapping.setDisplayName(requireText(command.displayName(), "CARRIER_DISPLAY_NAME_REQUIRED"));
        mapping.setProviderCode(normalizeRequired(command.providerCode(), "PROVIDER_CODE_REQUIRED"));
        mapping.setProviderCarrierCode(
                normalizeRequired(command.providerCarrierCode(), "PROVIDER_CARRIER_CODE_REQUIRED"));
        mapping.setPhoneRequirement(enumName(command.phoneRequirement(),
                RentalLogisticsPhoneRequirementEnum.class, RentalLogisticsPhoneRequirementEnum.OPTIONAL.name(),
                "PHONE_REQUIREMENT_INVALID"));
        mapping.setStatus(enumName(command.status(), RentalCarrierMappingStatusEnum.class,
                RentalCarrierMappingStatusEnum.DISABLED.name(), "CARRIER_MAPPING_STATUS_INVALID"));
        if (created) {
            mappingMapper.insert(mapping);
        } else {
            mappingMapper.updateById(mapping);
        }
        return toView(mapping);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCarrierMapping(Long id) {
        if (id == null) {
            throw new RentalLogisticsException("CARRIER_MAPPING_ID_REQUIRED");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalLogisticsCarrierMappingDO mapping = mappingMapper.selectByTenantIdAndId(tenantId, id);
        if (mapping == null) {
            throw new RentalLogisticsException("CARRIER_MAPPING_NOT_FOUND");
        }
        mappingMapper.deleteById(id);
    }

    private RentalLogisticsProviderConfigDO defaultConfig(String providerCode) {
        return RentalLogisticsProviderConfigDO.builder()
                .providerCode(providerCode)
                .enabled(false)
                .queryEnabled(false)
                .subscribeEnabled(false)
                .minimumQueryIntervalSeconds(RentalLogisticsProviderConfigService.MINIMUM_QUERY_INTERVAL_SECONDS)
                .resultVersion("4")
                .configStatus("INCOMPLETE")
                .build();
    }

    private ProviderConfigView toView(RentalLogisticsProviderConfigDO config,
                                      List<RentalLogisticsProviderCredentialDO> credentials) {
        String effectiveConfigStatus = effectiveProviderStatus(config, credentials);
        return new ProviderConfigView(config.getProviderCode(),
                Boolean.TRUE.equals(config.getEnabled()),
                Boolean.TRUE.equals(config.getQueryEnabled()),
                Boolean.TRUE.equals(config.getSubscribeEnabled()),
                StringUtils.hasText(config.getCallbackSecret()), mask(config.getCallbackSecret()),
                config.getCallbackBaseUrl(),
                config.getMinimumQueryIntervalSeconds() == null
                        ? RentalLogisticsProviderConfigService.MINIMUM_QUERY_INTERVAL_SECONDS
                        : Math.max(RentalLogisticsProviderConfigService.MINIMUM_QUERY_INTERVAL_SECONDS,
                        config.getMinimumQueryIntervalSeconds()),
                config.getResultVersion(), effectiveConfigStatus, config.getLastVerifiedAt(),
                credentials.stream().map(this::toView).toList());
    }

    private ProviderCredentialView toView(RentalLogisticsProviderCredentialDO credential) {
        return new ProviderCredentialView(credential.getId(), credential.getProviderCode(),
                credential.getCredentialName(), Boolean.TRUE.equals(credential.getEnabled()),
                credential.getSortOrder() == null ? 100 : credential.getSortOrder(),
                StringUtils.hasText(credential.getCustomerCode()), mask(credential.getCustomerCode()),
                StringUtils.hasText(credential.getApiKey()), mask(credential.getApiKey()),
                credential.getConfigStatus(), credential.getLastVerifiedAt());
    }

    private CarrierMappingView toView(RentalLogisticsCarrierMappingDO mapping) {
        return new CarrierMappingView(mapping.getId(), mapping.getSourceType(), mapping.getSourceCarrierCode(),
                mapping.getCanonicalCarrierCode(), mapping.getDisplayName(), mapping.getProviderCode(),
                mapping.getProviderCarrierCode(), mapping.getPhoneRequirement(), mapping.getStatus());
    }

    private boolean isGlobalConfigComplete(RentalLogisticsProviderConfigDO config) {
        return !Boolean.TRUE.equals(config.getSubscribeEnabled())
                || StringUtils.hasText(config.getCallbackBaseUrl());
    }

    private boolean isCredentialComplete(RentalLogisticsProviderCredentialDO credential) {
        return StringUtils.hasText(credential.getCustomerCode())
                && StringUtils.hasText(credential.getApiKey());
    }

    private boolean isProviderReady(RentalLogisticsProviderConfigDO config,
                                    List<RentalLogisticsProviderCredentialDO> credentials) {
        return isGlobalConfigComplete(config)
                && credentials.stream().anyMatch(credential ->
                Boolean.TRUE.equals(credential.getEnabled()) && isCredentialComplete(credential));
    }

    private String effectiveProviderStatus(RentalLogisticsProviderConfigDO config,
                                           List<RentalLogisticsProviderCredentialDO> credentials) {
        if (!isProviderReady(config, credentials)) {
            return "INCOMPLETE";
        }
        return "LOCALLY_VERIFIED".equals(config.getConfigStatus())
                ? "LOCALLY_VERIFIED" : "READY_UNVERIFIED";
    }

    private void refreshProviderStatus(Long tenantId, String providerCode) {
        RentalLogisticsProviderConfigDO config =
                configMapper.selectByProviderCodeForUpdate(tenantId, providerCode);
        if (config == null) {
            return;
        }
        List<RentalLogisticsProviderCredentialDO> credentials =
                credentialMapper.selectListByProvider(tenantId, providerCode);
        config.setConfigStatus(isProviderReady(config, credentials) ? "READY_UNVERIFIED" : "INCOMPLETE");
        configMapper.updateById(config);
    }

    private String applySecret(String current, SecretAction action, String replacement, String requiredCode) {
        SecretAction effectiveAction = action == null ? SecretAction.KEEP : action;
        return switch (effectiveAction) {
            case KEEP -> current;
            case CLEAR -> null;
            case REPLACE -> requireText(replacement, requiredCode);
        };
    }

    private void applyBoolean(Boolean value, java.util.function.Consumer<Boolean> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private String mask(String value) {
        return StringUtils.hasText(value) ? MASKED : null;
    }

    private String normalizeRequired(String value, String errorCode) {
        return requireText(value, errorCode).toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String errorCode) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new RentalLogisticsException(errorCode);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String validateCallbackBaseUrl(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            URI uri = new URI(trimmed);
            boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme());
            if (!validScheme || !StringUtils.hasText(uri.getHost())
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new RentalLogisticsException("CALLBACK_BASE_URL_INVALID");
            }
            return trimmed;
        } catch (URISyntaxException ex) {
            throw new RentalLogisticsException("CALLBACK_BASE_URL_INVALID");
        }
    }

    private <E extends Enum<E>> String enumName(String value, Class<E> type, String defaultValue, String errorCode) {
        String normalized = value == null ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new RentalLogisticsException(errorCode);
        }
    }
}
