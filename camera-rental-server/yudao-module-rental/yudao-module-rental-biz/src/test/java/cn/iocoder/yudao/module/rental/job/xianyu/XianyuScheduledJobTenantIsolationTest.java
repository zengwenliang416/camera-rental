package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushRetryService;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuScheduledJobTenantIsolationTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void quartzJobsMustNotFanOutGlobalCredentialsToEveryTenant() throws Exception {
        assertNull(XianyuOrderSyncJob.class.getMethod("execute", String.class).getAnnotation(TenantJob.class));
        assertNull(XianyuShopSyncJob.class.getMethod("execute", String.class).getAnnotation(TenantJob.class));
        assertNull(XianyuPushRetryJob.class.getMethod("execute", String.class).getAnnotation(TenantJob.class));
    }

    @Test
    void springSchedulerMustRunUnderConfiguredTenant() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setAppKey("runtime-app-key");
        properties.setAppSecret("runtime-app-secret");
        ReflectionTestUtils.setField(properties, "tenantId", 42L);
        XianyuChannelSyncService service = mock(XianyuChannelSyncService.class);
        when(service.syncAuthorizedShops()).thenAnswer(invocation -> {
            assertEquals(42L, TenantContextHolder.getTenantId());
            return "ok";
        });

        new XianyuChannelSyncScheduler(service, properties).scheduledShopSync();
    }

    @Test
    void springSchedulerMustSkipWhenIntegrationIsDisabled() {
        XianyuProperties properties = new XianyuProperties();
        XianyuChannelSyncService service = mock(XianyuChannelSyncService.class);

        new XianyuChannelSyncScheduler(service, properties).scheduledShopSync();

        verify(service, never()).syncAuthorizedShops();
    }

    @Test
    void pushRetryJobMustRunUnderConfiguredTenant() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setAppKey("runtime-app-key");
        properties.setAppSecret("runtime-app-secret");
        ReflectionTestUtils.setField(properties, "tenantId", 42L);
        XianyuPushRetryService service = mock(XianyuPushRetryService.class);
        when(service.retryStaleEvents()).thenAnswer(invocation -> {
            assertEquals(42L, TenantContextHolder.getTenantId());
            return "ok";
        });

        assertEquals("ok", new XianyuPushRetryJob(service, properties).execute(""));
    }

}
