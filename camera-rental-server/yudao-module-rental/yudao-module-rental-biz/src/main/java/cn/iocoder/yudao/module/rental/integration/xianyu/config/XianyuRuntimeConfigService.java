package cn.iocoder.yudao.module.rental.integration.xianyu.config;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuApplicationMapper;
import org.springframework.stereotype.Service;

@Service
public class XianyuRuntimeConfigService {

    private final XianyuApplicationMapper applicationMapper;

    public XianyuRuntimeConfigService(XianyuApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    public XianyuProperties getCurrent() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return toRuntime(applicationMapper.selectCurrentByTenantId(tenantId), tenantId);
    }

    public XianyuApplicationDO getCurrentApplication() {
        return applicationMapper.selectCurrentByTenantId(TenantContextHolder.getRequiredTenantId());
    }

    public XianyuProperties findByAppKey(String appKey) {
        XianyuApplicationDO application = TenantUtils.executeIgnore(() -> applicationMapper.selectByAppKey(appKey));
        return application == null ? null : toRuntime(application, application.getTenantId());
    }

    static XianyuProperties toRuntime(XianyuApplicationDO application, Long tenantId) {
        XianyuProperties result = new XianyuProperties();
        result.setTenantId(tenantId);
        if (application == null) {
            return result;
        }
        result.setEnabled(Boolean.TRUE.equals(application.getEnabled()));
        result.setBaseUrl(defaultString(application.getBaseUrl(), "https://open.goofish.pro"));
        result.setAppKey(application.getAppKey());
        result.setAppSecret(application.getAppSecret());
        result.setWebhookBaseUrl(application.getWebhookBaseUrl());
        result.setWriteEnabled(Boolean.TRUE.equals(application.getWriteEnabled()));
        XianyuProperties.Job job = result.getJob();
        job.setEnabled(Boolean.TRUE.equals(application.getJobEnabled()));
        job.setLookbackDays(defaultInt(application.getLookbackDays(), 7));
        job.setOverlapMinutes(defaultInt(application.getOverlapMinutes(), 10));
        job.setMaxPagesPerShop(defaultInt(application.getMaxPagesPerShop(), 20));
        job.setPageSize(defaultInt(application.getPageSize(), 50));
        job.setPushRetryStaleSeconds(defaultInt(application.getPushRetryStaleSeconds(), 120));
        job.setPushRetryBatchSize(defaultInt(application.getPushRetryBatchSize(), 100));
        return result;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

}
