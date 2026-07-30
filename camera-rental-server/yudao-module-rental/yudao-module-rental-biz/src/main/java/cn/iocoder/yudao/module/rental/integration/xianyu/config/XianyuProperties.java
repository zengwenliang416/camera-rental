package cn.iocoder.yudao.module.rental.integration.xianyu.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * Tenant-scoped XianGuanJia runtime snapshot loaded from encrypted persistence.
 */
@Data
@ToString(exclude = "appSecret")
public class XianyuProperties {

    private boolean enabled = false;

    private String baseUrl = "https://open.goofish.pro";

    private String appKey;

    private String appSecret;

    private String webhookBaseUrl;

    /**
     * Explicit switch for XianGuanJia write APIs. Read integration can be ready
     * while writes remain disabled.
     */
    private boolean writeEnabled = false;

    /**
     * Tenant that owns the persisted XianGuanJia application.
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
        private boolean enabled = false;
        /** First-time lookback when a shop has no ORDER cursor. */
        private int lookbackDays = 7;
        /** Overlap minutes before cursor to avoid missing boundary updates. */
        private int overlapMinutes = 10;
        /** Max list pages per shop per order job run. */
        private int maxPagesPerShop = 20;
        /** Order list page size. */
        private int pageSize = 50;
        /** Ignore fresh events so the normal after-commit consumer can finish first. */
        private int pushRetryStaleSeconds = 120;
        /** Max push events queued by one recovery run. */
        private int pushRetryBatchSize = 100;
    }

}
