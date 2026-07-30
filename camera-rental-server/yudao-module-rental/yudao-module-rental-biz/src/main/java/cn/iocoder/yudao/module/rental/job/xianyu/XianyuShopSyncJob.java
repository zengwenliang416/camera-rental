package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import org.springframework.stereotype.Component;

/**
 * Quartz / infra job: sync authorized XianGuanJia shops.
 * Register in 基础设施 → 定时任务, handlerName = xianyuShopSyncJob
 */
@Component("xianyuShopSyncJob")
public class XianyuShopSyncJob implements JobHandler {

    private final XianyuChannelSyncService channelSyncService;
    private final XianyuTenantJobGuard jobGuard;

    public XianyuShopSyncJob(XianyuChannelSyncService channelSyncService, XianyuTenantJobGuard jobGuard) {
        this.channelSyncService = channelSyncService;
        this.jobGuard = jobGuard;
    }

    @Override
    @TenantJob
    public String execute(String param) {
        return jobGuard.execute(channelSyncService::syncAuthorizedShops);
    }

}
