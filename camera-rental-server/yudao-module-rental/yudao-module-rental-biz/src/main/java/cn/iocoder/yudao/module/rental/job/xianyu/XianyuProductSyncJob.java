package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import org.springframework.stereotype.Component;

/**
 * Quartz / infra job: incremental product sync for all VALID shops.
 * Register in 基础设施 -> 定时任务, handlerName = xianyuProductSyncJob
 */
@Component("xianyuProductSyncJob")
public class XianyuProductSyncJob implements JobHandler {

    private final XianyuChannelSyncService channelSyncService;
    private final XianyuProperties properties;

    public XianyuProductSyncJob(XianyuChannelSyncService channelSyncService, XianyuProperties properties) {
        this.channelSyncService = channelSyncService;
        this.properties = properties;
    }

    @Override
    public String execute(String param) {
        return TenantUtils.execute(properties.requireTenantId(), channelSyncService::syncProductsIncremental);
    }

}
