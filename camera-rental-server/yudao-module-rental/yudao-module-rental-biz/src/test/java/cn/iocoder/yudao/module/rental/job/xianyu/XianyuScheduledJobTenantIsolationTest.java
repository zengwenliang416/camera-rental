package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushRetryService;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuChannelSyncService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuScheduledJobTenantIsolationTest {

    @Test
    void everyXianyuQuartzJobMustExecuteThroughTenantAspect() throws Exception {
        for (Class<?> type : List.of(XianyuOrderSyncJob.class, XianyuShopSyncJob.class,
                XianyuProductSyncJob.class, XianyuAfterSaleSyncJob.class, XianyuPushRetryJob.class)) {
            Method execute = type.getMethod("execute", String.class);
            assertNotNull(execute.getAnnotation(TenantJob.class), type.getSimpleName());
        }
    }

    @Test
    void tenantJobGuardMustSkipDisabledTenantConfiguration() {
        XianyuRuntimeConfigService runtimeConfigService = mock(XianyuRuntimeConfigService.class);
        XianyuChannelSyncService service = mock(XianyuChannelSyncService.class);
        when(runtimeConfigService.getCurrent()).thenReturn(new XianyuProperties());
        XianyuTenantJobGuard guard = new XianyuTenantJobGuard(runtimeConfigService);

        String result = new XianyuShopSyncJob(service, guard).execute("");

        assertEquals("skip: status=DISABLED jobEnabled=false", result);
        verify(service, never()).syncAuthorizedShops();
    }

    @Test
    void tenantJobGuardMustUseCurrentTenantDatabaseConfiguration() {
        XianyuRuntimeConfigService runtimeConfigService = mock(XianyuRuntimeConfigService.class);
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setAppKey("runtime-app-key");
        properties.setAppSecret("runtime-app-secret");
        properties.getJob().setEnabled(true);
        when(runtimeConfigService.getCurrent()).thenReturn(properties);
        XianyuPushRetryService retryService = mock(XianyuPushRetryService.class);
        when(retryService.retryStaleEvents()).thenReturn("ok");

        String result = new XianyuPushRetryJob(
                retryService, new XianyuTenantJobGuard(runtimeConfigService)).execute("");

        assertEquals("ok", result);
        verify(retryService).retryStaleEvents();
    }

}
