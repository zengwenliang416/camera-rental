package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigUpdateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuApplicationMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_CONFIG_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isA;

class XianyuConfigAdminServiceTest {

    private final XianyuApplicationMapper applicationMapper = mock(XianyuApplicationMapper.class);
    private final XianyuRuntimeConfigService runtimeConfigService = mock(XianyuRuntimeConfigService.class);
    private final XianyuConfigAdminService service =
            new XianyuConfigAdminService(applicationMapper, runtimeConfigService);

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldMaskAppKeyAndNeverExposeSecret() {
        XianyuProperties properties = readyProperties();
        properties.setAppKey("demo-app-key-12345678");
        properties.setAppSecret("runtime-secret-value");
        when(runtimeConfigService.getCurrent()).thenReturn(properties);

        XianyuConfigRespVO vo = service.getConfig();

        assertEquals("READY", vo.getStatus());
        assertTrue(Boolean.TRUE.equals(vo.getAppSecretConfigured()));
        assertEquals("demo****5678", vo.getAppKeyMasked());
        assertFalse(vo.toString().contains("runtime-secret-value"));
        assertEquals("https://open.goofish.pro", vo.getBaseUrl());
    }

    @Test
    void shouldInsertTenantOwnedConfiguration() {
        when(runtimeConfigService.getCurrentApplication()).thenReturn(null);

        service.updateConfig(updateRequest());

        ArgumentCaptor<XianyuApplicationDO> captor = ArgumentCaptor.forClass(XianyuApplicationDO.class);
        verify(applicationMapper).insert(captor.capture());
        XianyuApplicationDO saved = captor.getValue();
        assertEquals(9L, saved.getTenantId());
        assertEquals("default", saved.getApplicationCode());
        assertEquals("new-app-key", saved.getAppKey());
        assertEquals("new-app-secret", saved.getAppSecret());
        assertEquals(Boolean.TRUE, saved.getEnabled());
        assertEquals(Boolean.TRUE, saved.getWriteEnabled());
        assertEquals(Boolean.TRUE, saved.getJobEnabled());
    }

    @Test
    void shouldKeepPersistedCredentialsWhenReplacementFieldsAreBlank() {
        XianyuApplicationDO existing = XianyuApplicationDO.builder()
                .id(10L)
                .applicationCode("primary")
                .displayName("Primary")
                .appKey("persisted-key")
                .appSecret("persisted-secret")
                .authorizationStatus("VALID")
                .build();
        existing.setTenantId(9L);
        when(runtimeConfigService.getCurrentApplication()).thenReturn(existing);
        XianyuConfigUpdateReqVO request = updateRequest();
        request.setAppKey(" ");
        request.setAppSecret("");

        service.updateConfig(request);

        ArgumentCaptor<XianyuApplicationDO> captor = ArgumentCaptor.forClass(XianyuApplicationDO.class);
        verify(applicationMapper).updateById(captor.capture());
        assertEquals(10L, captor.getValue().getId());
        assertEquals("persisted-key", captor.getValue().getAppKey());
        assertEquals("persisted-secret", captor.getValue().getAppSecret());
        assertEquals("VALID", captor.getValue().getAuthorizationStatus());
        verify(applicationMapper, never()).insert(captor.getValue());
    }

    @Test
    void shouldUpdateOnlyCurrentTenantApplicationWhenAppKeyChanges() {
        XianyuApplicationDO current = XianyuApplicationDO.builder()
                .id(10L)
                .applicationCode("old-app")
                .displayName("Old")
                .authorizationStatus("VALID")
                .build();
        when(runtimeConfigService.getCurrentApplication()).thenReturn(current);

        service.updateConfig(updateRequest());

        ArgumentCaptor<XianyuApplicationDO> captor = ArgumentCaptor.forClass(XianyuApplicationDO.class);
        verify(applicationMapper).updateById(captor.capture());
        assertEquals(10L, captor.getValue().getId());
        assertEquals("old-app", captor.getValue().getApplicationCode());
        assertEquals("new-app-key", captor.getValue().getAppKey());
        assertEquals("new-app-secret", captor.getValue().getAppSecret());
        assertEquals("VALID", captor.getValue().getAuthorizationStatus());
    }

    @Test
    void shouldRejectWriteEnablementBeforePersistenceWhenIntegrationIsDisabled() {
        XianyuConfigUpdateReqVO request = updateRequest();
        request.setEnabled(false);
        request.setWriteEnabled(true);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateConfig(request));

        assertEquals(XIANYU_CONFIG_INVALID.getCode(), exception.getCode());
        verify(applicationMapper, never()).insert(isA(XianyuApplicationDO.class));
        verify(applicationMapper, never()).updateById(isA(XianyuApplicationDO.class));
    }

    @Test
    void appSecretFieldMustUseEncryptionTypeHandler() throws Exception {
        Field field = XianyuApplicationDO.class.getDeclaredField("appSecret");
        TableField annotation = field.getAnnotation(TableField.class);

        assertEquals(EncryptTypeHandler.class, annotation.typeHandler());
    }

    @Test
    void shouldReportDisabledByDefault() {
        when(runtimeConfigService.getCurrent()).thenReturn(new XianyuProperties());

        XianyuConfigRespVO vo = service.getConfig();

        assertEquals("DISABLED", vo.getStatus());
        assertFalse(Boolean.TRUE.equals(vo.getEnabled()));
        assertFalse(Boolean.TRUE.equals(vo.getAppSecretConfigured()));
        assertNull(vo.getWebhookBaseUrl());
    }

    @Test
    void maskAppKeyHelpers() {
        assertEquals("", XianyuConfigAdminService.maskAppKey(null));
        assertEquals("****", XianyuConfigAdminService.maskAppKey("ab"));
        assertEquals("****", XianyuConfigAdminService.maskAppKey("abcd"));
        assertEquals("ab****ef", XianyuConfigAdminService.maskAppKey("abcdef"));
    }

    private XianyuConfigUpdateReqVO updateRequest() {
        XianyuConfigUpdateReqVO request = new XianyuConfigUpdateReqVO();
        request.setEnabled(true);
        request.setBaseUrl("https://open.goofish.pro/");
        request.setAppKey(" new-app-key ");
        request.setAppSecret(" new-app-secret ");
        request.setWebhookBaseUrl("https://rental.example.com/xianyu/webhook/");
        request.setWriteEnabled(true);
        request.setJobEnabled(true);
        request.setLookbackDays(14);
        request.setOverlapMinutes(15);
        request.setMaxPagesPerShop(30);
        request.setPageSize(80);
        request.setPushRetryStaleSeconds(180);
        request.setPushRetryBatchSize(200);
        return request;
    }

    private XianyuProperties readyProperties() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://open.goofish.pro");
        properties.setAppKey("demo-app");
        properties.setAppSecret("demo-secret");
        properties.setTenantId(9L);
        return properties;
    }

}
