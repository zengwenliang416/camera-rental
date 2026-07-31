package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsCallbackCommand;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsProvider;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsQueryCommand;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsSubscribeCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderConfigService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderCredentialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Kuaidi100LogisticsProvider implements LogisticsProvider {

    public static final String PROVIDER_CODE = "KUAIDI100";

    private final RentalLogisticsProviderConfigService configService;
    private final RentalLogisticsProviderCredentialService credentialService;
    private final Kuaidi100Gateway gateway;
    private final Kuaidi100Signer signer;
    private final Kuaidi100Converter converter;
    private final ObjectMapper objectMapper;

    public Kuaidi100LogisticsProvider(RentalLogisticsProviderConfigService configService,
                                     RentalLogisticsProviderCredentialService credentialService,
                                     Kuaidi100Gateway gateway,
                                     Kuaidi100Signer signer,
                                     Kuaidi100Converter converter,
                                     ObjectMapper objectMapper) {
        this.configService = configService;
        this.credentialService = credentialService;
        this.gateway = gateway;
        this.signer = signer;
        this.converter = converter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public LogisticsOperationResult subscribe(LogisticsSubscribeCommand command) {
        RentalLogisticsProviderConfigDO config = configService.get(PROVIDER_CODE);
        if (!enabled(config) || !Boolean.TRUE.equals(config.getSubscribeEnabled())) {
            return LogisticsOperationResult.failure("PROVIDER_DISABLED", false);
        }
        RentalLogisticsProviderCredentialDO credential = credentialService.get(command.credentialId());
        if (!credentialService.isUsable(credential, PROVIDER_CODE)) {
            return LogisticsOperationResult.failure("PROVIDER_CREDENTIAL_REQUIRED", false);
        }
        if (!StringUtils.hasText(command.callbackUrl()) || !StringUtils.hasText(command.callbackSalt())) {
            return LogisticsOperationResult.failure("CALLBACK_URL_REQUIRED", false);
        }
        try {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("callbackurl", command.callbackUrl());
            parameters.put("salt", command.callbackSalt());
            parameters.put("resultv2", resultVersion(config));
            if (StringUtils.hasText(command.trackingPhone())) {
                parameters.put("phone", command.trackingPhone());
            }
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("company", command.carrierCode());
            param.put("number", command.waybillNo());
            param.put("key", credential.getApiKey());
            param.put("parameters", parameters);
            String json = objectMapper.writeValueAsString(param);
            return converter.parseSubscribe(gateway.subscribe(Map.of("schema", "json", "param", json)));
        } catch (IOException exception) {
            return LogisticsOperationResult.failure("KUAIDI100_NETWORK_ERROR", true);
        }
    }

    @Override
    public LogisticsOperationResult query(LogisticsQueryCommand command) {
        RentalLogisticsProviderConfigDO config = configService.get(PROVIDER_CODE);
        if (!enabled(config) || !Boolean.TRUE.equals(config.getQueryEnabled())) {
            return LogisticsOperationResult.failure("PROVIDER_DISABLED", false);
        }
        RentalLogisticsProviderCredentialDO credential = credentialService.get(command.credentialId());
        if (!credentialService.isUsable(credential, PROVIDER_CODE)) {
            return LogisticsOperationResult.failure("PROVIDER_CREDENTIAL_REQUIRED", false);
        }
        try {
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("com", command.carrierCode());
            param.put("num", command.waybillNo());
            if (StringUtils.hasText(command.trackingPhone())) {
                param.put("phone", command.trackingPhone());
            }
            param.put("resultv2", resultVersion(config));
            param.put("show", "0");
            param.put("order", "asc");
            String json = objectMapper.writeValueAsString(param);
            String sign = signer.signQuery(json, credential.getApiKey(), credential.getCustomerCode());
            Map<String, String> form = new LinkedHashMap<>();
            form.put("customer", credential.getCustomerCode());
            form.put("sign", sign);
            form.put("param", json);
            return converter.parseQuery(gateway.query(form), "QUERY", null);
        } catch (IOException exception) {
            return LogisticsOperationResult.failure("KUAIDI100_NETWORK_ERROR", true);
        }
    }

    @Override
    public LogisticsOperationResult parseVerifiedCallback(LogisticsCallbackCommand command) {
        return converter.parseQuery(command.verifiedPayload(), "CALLBACK", command.inboxId());
    }

    private boolean enabled(RentalLogisticsProviderConfigDO config) {
        return config != null && Boolean.TRUE.equals(config.getEnabled());
    }

    private String resultVersion(RentalLogisticsProviderConfigDO config) {
        return StringUtils.hasText(config.getResultVersion()) ? config.getResultVersion() : "4";
    }
}
