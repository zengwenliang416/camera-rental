package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushRetryService;
import org.springframework.stereotype.Component;

@Component("xianyuPushRetryJob")
public class XianyuPushRetryJob implements JobHandler {

    private final XianyuPushRetryService retryService;
    private final XianyuProperties properties;

    public XianyuPushRetryJob(XianyuPushRetryService retryService, XianyuProperties properties) {
        this.retryService = retryService;
        this.properties = properties;
    }

    @Override
    public String execute(String param) {
        return TenantUtils.execute(properties.requireTenantId(), retryService::retryStaleEvents);
    }

}
