package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spring schedule fallback when Quartz is off. Disabled by default once infra jobs are used.
 * Runs only under the tenant that owns the process-level channel credentials.
 */
@Component
@ConditionalOnProperty(prefix = "rental.xianyu.job", name = "spring-schedule-enabled", havingValue = "true")
@Slf4j
public class XianyuChannelSyncScheduler {

    private final XianyuChannelSyncService channelSyncService;
    private final XianyuProperties properties;

    public XianyuChannelSyncScheduler(XianyuChannelSyncService channelSyncService, XianyuProperties properties) {
        this.channelSyncService = channelSyncService;
        this.properties = properties;
    }

    @Scheduled(cron = "${rental.xianyu.job.shop-cron:0 0/30 * * * ?}")
    public void scheduledShopSync() {
        if (!properties.getJob().isEnabled()
                || properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return;
        }
        TenantUtils.execute(properties.requireTenantId(), () -> {
            try {
                String result = channelSyncService.syncAuthorizedShops();
                log.info("[xianyu][schedule] shop sync: {}", result);
            } catch (Exception ex) {
                log.warn("[xianyu][schedule] shop sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
        });
    }

    @Scheduled(cron = "${rental.xianyu.job.order-cron:0 0/5 * * * ?}")
    public void scheduledOrderSync() {
        if (!properties.getJob().isEnabled()
                || properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return;
        }
        TenantUtils.execute(properties.requireTenantId(), () -> {
            try {
                String result = channelSyncService.syncOrdersIncremental();
                log.info("[xianyu][schedule] order sync: {}", result);
            } catch (Exception ex) {
                log.warn("[xianyu][schedule] order sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
        });
    }

    @Scheduled(cron = "${rental.xianyu.job.product-cron:0 0/10 * * * ?}")
    public void scheduledProductSync() {
        if (!properties.getJob().isEnabled()
                || properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return;
        }
        TenantUtils.execute(properties.requireTenantId(), () -> {
            try {
                String result = channelSyncService.syncProductsIncremental();
                log.info("[xianyu][schedule] product sync: {}", result);
            } catch (Exception ex) {
                log.warn("[xianyu][schedule] product sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
        });
    }

    @Scheduled(cron = "${rental.xianyu.job.after-sale-cron:0 0/10 * * * ?}")
    public void scheduledAfterSaleSync() {
        if (!properties.getJob().isEnabled()
                || properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return;
        }
        TenantUtils.execute(properties.requireTenantId(), () -> {
            try {
                String result = channelSyncService.syncAfterSalesIncremental();
                log.info("[xianyu][schedule] after-sale sync: {}", result);
            } catch (Exception ex) {
                log.warn("[xianyu][schedule] after-sale sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
        });
    }

}
