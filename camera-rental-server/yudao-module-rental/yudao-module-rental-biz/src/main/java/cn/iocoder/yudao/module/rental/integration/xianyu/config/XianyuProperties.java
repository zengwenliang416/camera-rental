package cn.iocoder.yudao.module.rental.integration.xianyu.config;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * 闲管家运行时配置。敏感值仅允许由环境变量或配置中心注入。
 */
@ConfigurationProperties(prefix = "rental.xianyu")
@Validated
@Data
public class XianyuProperties {

    private boolean enabled = false;

    private String baseUrl = "https://open.goofish.pro";

    private String appKey;

    private String appSecret;

    private String webhookBaseUrl;

    /**
     * Tenant that owns the process-level XianGuanJia credentials.
     */
    private Long tenantId;

    /**
     * Scheduled channel sync. When disabled, only manual admin APIs sync.
     */
    private Job job = new Job();

    public IntegrationStatus getIntegrationStatus() {
        if (!enabled) {
            return IntegrationStatus.DISABLED;
        }
        return StringUtils.hasText(appKey) && StringUtils.hasText(appSecret)
                ? IntegrationStatus.READY : IntegrationStatus.MISSING_CREDENTIALS;
    }

    @AssertTrue(message = "rental.xianyu.tenant-id must be a positive integer when the integration is enabled")
    public boolean isTenantConfigurationValid() {
        return !enabled || tenantId != null && tenantId > 0;
    }

    public long requireTenantId() {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("XianGuanJia tenant id is not configured");
        }
        return tenantId;
    }

    public enum IntegrationStatus {
        DISABLED,
        MISSING_CREDENTIALS,
        READY
    }

    @Data
    public static class Job {
        /**
         * Master switch for scheduled shop/order sync. Defaults on; still no-ops when integration is not READY.
         */
        private boolean enabled = true;
        /** Spring cron: authorized shops. Default every 30 minutes. */
        private String shopCron = "0 0/30 * * * ?";
        /** Spring cron: order incremental sync. Default every 1 minute. */
        private String orderCron = "0 * * * * ?";
        /** Spring cron: product incremental sync. Default every 10 minutes. */
        private String productCron = "0 0/10 * * * ?";
        /** Spring cron: after-sale incremental sync. Default every 10 minutes. */
        private String afterSaleCron = "0 0/10 * * * ?";
        /** First-time lookback when a shop has no ORDER cursor. */
        private int lookbackDays = 7;
        /** Overlap minutes before cursor to avoid missing boundary updates. */
        private int overlapMinutes = 10;
        /** Max list pages per shop per order job run. */
        private int maxPagesPerShop = 20;
        /** Order list page size. */
        private int pageSize = 50;
        /** Retry durable push events that were not completed. */
        private String pushRetryCron = "0 0/5 * * * ?";
        /** Ignore fresh events so the normal after-commit consumer can finish first. */
        private int pushRetryStaleSeconds = 120;
        /** Max push events queued by one recovery run. */
        private int pushRetryBatchSize = 100;
        /**
         * When true, also run Spring {@code @Scheduled} fallback (useful if Quartz is excluded).
         * Prefer infra Job + Quartz in normal environments; default false after infra registration.
         */
        private boolean springScheduleEnabled = false;
        /** Run a blocking one-shot shop/order sync during application startup. Prefer false. */
        private boolean startupSyncEnabled = false;
        /** Register handlers into infra_job + Quartz on startup if missing. */
        private boolean registerInfraJobs = true;
    }

}
