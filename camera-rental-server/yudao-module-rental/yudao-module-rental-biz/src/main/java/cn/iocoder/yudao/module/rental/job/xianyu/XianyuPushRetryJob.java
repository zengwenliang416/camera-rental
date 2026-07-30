package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushRetryService;
import org.springframework.stereotype.Component;

@Component("xianyuPushRetryJob")
public class XianyuPushRetryJob implements JobHandler {

    private final XianyuPushRetryService retryService;
    private final XianyuTenantJobGuard jobGuard;

    public XianyuPushRetryJob(XianyuPushRetryService retryService, XianyuTenantJobGuard jobGuard) {
        this.retryService = retryService;
        this.jobGuard = jobGuard;
    }

    @Override
    @TenantJob
    public String execute(String param) {
        return jobGuard.execute(retryService::retryStaleEvents);
    }

}
