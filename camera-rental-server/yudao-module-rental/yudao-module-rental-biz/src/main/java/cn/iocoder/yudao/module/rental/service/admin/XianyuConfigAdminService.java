package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigRespVO;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Exposes redacted integration status. Never returns AppSecret.
 */
@Service
public class XianyuConfigAdminService {

    private final XianyuProperties properties;

    public XianyuConfigAdminService(XianyuProperties properties) {
        this.properties = properties;
    }

    public XianyuConfigRespVO getConfig() {
        XianyuConfigRespVO vo = new XianyuConfigRespVO();
        vo.setEnabled(properties.isEnabled());
        vo.setBaseUrl(properties.getBaseUrl());
        vo.setStatus(properties.getIntegrationStatus().name());
        vo.setAppKeyMasked(maskAppKey(properties.getAppKey()));
        vo.setAppSecretConfigured(StringUtils.hasText(properties.getAppSecret()));
        vo.setWebhookBaseUrlConfigured(StringUtils.hasText(properties.getWebhookBaseUrl()));
        return vo;
    }

    static String maskAppKey(String appKey) {
        if (!StringUtils.hasText(appKey)) {
            return "";
        }
        String value = appKey.trim();
        if (value.length() <= 4) {
            return "****";
        }
        if (value.length() <= 8) {
            return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

}
