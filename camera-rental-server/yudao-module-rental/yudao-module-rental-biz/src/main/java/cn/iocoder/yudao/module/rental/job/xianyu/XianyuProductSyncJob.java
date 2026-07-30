package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import org.springframework.stereotype.Component;

/**
 * Quartz / infra job: incremental product sync for all VALID shops.
 * Register in 基础设施 -> 定时任务, handlerName = xianyuProductSyncJob
 */
@Component("xianyuProductSyncJob")
public class XianyuProductSyncJob implements JobHandler {

    private final XianyuChannelSyncService channelSyncService;
    private final XianyuTenantJobGuard jobGuard;

    public XianyuProductSyncJob(XianyuChannelSyncService channelSyncService, XianyuTenantJobGuard jobGuard) {
        this.channelSyncService = channelSyncService;
        this.jobGuard = jobGuard;
    }

    @Override
    @TenantJob
    public String execute(String param) {
        return jobGuard.execute(channelSyncService::syncProductsIncremental);
    }

}
