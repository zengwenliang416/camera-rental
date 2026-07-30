package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigUpdateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuApplicationMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_CONFIG_INVALID;

/**
 * Exposes redacted integration status. Never returns AppSecret.
 */
@Service
public class XianyuConfigAdminService {

    private final XianyuApplicationMapper applicationMapper;
    private final XianyuRuntimeConfigService runtimeConfigService;

    public XianyuConfigAdminService(XianyuApplicationMapper applicationMapper,
                                    XianyuRuntimeConfigService runtimeConfigService) {
        this.applicationMapper = applicationMapper;
        this.runtimeConfigService = runtimeConfigService;
    }

    public XianyuConfigRespVO getConfig() {
        return toResponse(runtimeConfigService.getCurrent());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(XianyuConfigUpdateReqVO reqVO) {
        XianyuApplicationDO existing = runtimeConfigService.getCurrentApplication();
        String appKey = StringUtils.hasText(reqVO.getAppKey())
                ? reqVO.getAppKey().trim() : existing == null ? null : existing.getAppKey();
        String appSecret = StringUtils.hasText(reqVO.getAppSecret())
                ? reqVO.getAppSecret().trim() : existing == null ? null : existing.getAppSecret();
        String baseUrl = validateHttpUrl(reqVO.getBaseUrl(), "接口地址");
        String webhookBaseUrl = StringUtils.hasText(reqVO.getWebhookBaseUrl())
                ? validateHttpUrl(reqVO.getWebhookBaseUrl(), "Webhook 地址") : null;
        if (Boolean.TRUE.equals(reqVO.getEnabled())
                && (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret))) {
            throw exception(XIANYU_CONFIG_INVALID, "启用集成前必须配置 AppKey 和 AppSecret");
        }
        if (Boolean.TRUE.equals(reqVO.getWriteEnabled()) && !Boolean.TRUE.equals(reqVO.getEnabled())) {
            throw exception(XIANYU_CONFIG_INVALID, "启用真实写操作前必须先启用闲管家集成");
        }

        XianyuApplicationDO application = XianyuApplicationDO.builder()
                .id(existing == null ? null : existing.getId())
                .applicationCode(existing == null ? "default" : existing.getApplicationCode())
                .displayName(existing == null ? "XianGuanJia" : existing.getDisplayName())
                .enabled(reqVO.getEnabled())
                .baseUrl(baseUrl)
                .appKey(appKey)
                .appSecret(appSecret)
                .webhookBaseUrl(webhookBaseUrl)
                .writeEnabled(reqVO.getWriteEnabled())
                .jobEnabled(reqVO.getJobEnabled())
                .lookbackDays(defaultInt(reqVO.getLookbackDays(), 7))
                .overlapMinutes(defaultInt(reqVO.getOverlapMinutes(), 10))
                .maxPagesPerShop(defaultInt(reqVO.getMaxPagesPerShop(), 20))
                .pageSize(defaultInt(reqVO.getPageSize(), 50))
                .pushRetryStaleSeconds(defaultInt(reqVO.getPushRetryStaleSeconds(), 120))
                .pushRetryBatchSize(defaultInt(reqVO.getPushRetryBatchSize(), 100))
                .authorizationStatus(existing == null ? "UNKNOWN" : existing.getAuthorizationStatus())
                .authorizationExpiresAt(existing == null ? null : existing.getAuthorizationExpiresAt())
                .build();
        application.setTenantId(TenantContextHolder.getRequiredTenantId());
        if (existing == null) {
            applicationMapper.insert(application);
        } else {
            applicationMapper.updateById(application);
        }
    }

    private XianyuConfigRespVO toResponse(XianyuProperties properties) {
        XianyuConfigRespVO vo = new XianyuConfigRespVO();
        vo.setEnabled(properties.isEnabled());
        vo.setBaseUrl(properties.getBaseUrl());
        vo.setStatus(properties.getIntegrationStatus().name());
        vo.setAppKeyMasked(maskAppKey(properties.getAppKey()));
        vo.setAppSecretConfigured(StringUtils.hasText(properties.getAppSecret()));
        vo.setWebhookBaseUrlConfigured(StringUtils.hasText(properties.getWebhookBaseUrl()));
        vo.setWebhookBaseUrl(properties.getWebhookBaseUrl());
        vo.setWriteEnabled(properties.isWriteEnabled());
        XianyuProperties.Job job = properties.getJob();
        vo.setJobEnabled(job.isEnabled());
        vo.setLookbackDays(job.getLookbackDays());
        vo.setOverlapMinutes(job.getOverlapMinutes());
        vo.setMaxPagesPerShop(job.getMaxPagesPerShop());
        vo.setPageSize(job.getPageSize());
        vo.setPushRetryStaleSeconds(job.getPushRetryStaleSeconds());
        vo.setPushRetryBatchSize(job.getPushRetryBatchSize());
        return vo;
    }

    private String validateHttpUrl(String raw, String label) {
        if (!StringUtils.hasText(raw)) {
            throw exception(XIANYU_CONFIG_INVALID, label + "不能为空");
        }
        try {
            URI uri = new URI(raw.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())
                    || uri.getUserInfo() != null) {
                throw exception(XIANYU_CONFIG_INVALID, label + "必须是无用户信息的 HTTPS 地址");
            }
            return uri.toString().replaceAll("/+$", "");
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw exception(XIANYU_CONFIG_INVALID, label + "格式不正确");
        }
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
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
