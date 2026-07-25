package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigRespVO;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianyuConfigAdminServiceTest {

    @Test
    void shouldMaskAppKeyAndNeverExposeSecret() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://open.goofish.pro");
        properties.setAppKey("demo-app-key-12345678");
        properties.setAppSecret("runtime-secret-value");
        XianyuConfigAdminService service = new XianyuConfigAdminService(properties);

        XianyuConfigRespVO vo = service.getConfig();

        assertEquals("READY", vo.getStatus());
        assertTrue(Boolean.TRUE.equals(vo.getAppSecretConfigured()));
        assertEquals("demo****5678", vo.getAppKeyMasked());
        assertFalse(vo.toString().contains("runtime-secret-value"));
        assertEquals("https://open.goofish.pro", vo.getBaseUrl());
    }

    @Test
    void shouldReportDisabledByDefault() {
        XianyuConfigAdminService service = new XianyuConfigAdminService(new XianyuProperties());
        XianyuConfigRespVO vo = service.getConfig();
        assertEquals("DISABLED", vo.getStatus());
        assertFalse(Boolean.TRUE.equals(vo.getEnabled()));
        assertFalse(Boolean.TRUE.equals(vo.getAppSecretConfigured()));
    }

    @Test
    void maskAppKeyHelpers() {
        assertEquals("", XianyuConfigAdminService.maskAppKey(null));
        assertEquals("****", XianyuConfigAdminService.maskAppKey("ab"));
        assertEquals("****", XianyuConfigAdminService.maskAppKey("abcd"));
        assertEquals("ab****ef", XianyuConfigAdminService.maskAppKey("abcdef"));
    }

}
