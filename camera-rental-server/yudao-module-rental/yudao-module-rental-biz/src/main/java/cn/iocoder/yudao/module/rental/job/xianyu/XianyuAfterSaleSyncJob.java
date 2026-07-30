package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import org.springframework.stereotype.Component;

/**
 * Quartz / infra job: incremental after-sale sync for all VALID shops.
 * Register in 基础设施 -> 定时任务, handlerName = xianyuAfterSaleSyncJob
 */
@Component("xianyuAfterSaleSyncJob")
public class XianyuAfterSaleSyncJob implements JobHandler {

    private final XianyuChannelSyncService channelSyncService;
    private final XianyuTenantJobGuard jobGuard;

    public XianyuAfterSaleSyncJob(XianyuChannelSyncService channelSyncService, XianyuTenantJobGuard jobGuard) {
        this.channelSyncService = channelSyncService;
        this.jobGuard = jobGuard;
    }

    @Override
    @TenantJob
    public String execute(String param) {
        return jobGuard.execute(channelSyncService::syncAfterSalesIncremental);
    }

}
