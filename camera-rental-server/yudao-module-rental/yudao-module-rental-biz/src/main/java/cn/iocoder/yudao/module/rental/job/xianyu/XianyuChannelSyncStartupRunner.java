package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * One-shot bootstrap after process start so operators do not wait for the first cron tick.
 */
@Component
@Order(1000)
@ConditionalOnProperty(prefix = "rental.xianyu.job", name = "startup-sync-enabled", havingValue = "true")
@Slf4j
public class XianyuChannelSyncStartupRunner implements ApplicationRunner {

    private final XianyuChannelSyncService channelSyncService;
    private final XianyuProperties properties;

    public XianyuChannelSyncStartupRunner(XianyuChannelSyncService channelSyncService,
                                          XianyuProperties properties) {
        this.channelSyncService = channelSyncService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getJob().isEnabled()) {
            return;
        }
        if (properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            log.info("[xianyu][startup] skip bootstrap sync: {}", properties.getIntegrationStatus());
            return;
        }
        TenantUtils.execute(properties.requireTenantId(), () -> {
            try {
                log.info("[xianyu][startup] shop sync: {}", channelSyncService.syncAuthorizedShops());
            } catch (Exception ex) {
                log.warn("[xianyu][startup] shop sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
            try {
                log.info("[xianyu][startup] order sync: {}", channelSyncService.syncOrdersIncremental());
            } catch (Exception ex) {
                log.warn("[xianyu][startup] order sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
            try {
                log.info("[xianyu][startup] product sync: {}", channelSyncService.syncProductsIncremental());
            } catch (Exception ex) {
                log.warn("[xianyu][startup] product sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
            try {
                log.info("[xianyu][startup] after-sale sync: {}", channelSyncService.syncAfterSalesIncremental());
            } catch (Exception ex) {
                log.warn("[xianyu][startup] after-sale sync failed code={}", XianyuSafeErrorCode.from(ex));
            }
        });
    }

}
