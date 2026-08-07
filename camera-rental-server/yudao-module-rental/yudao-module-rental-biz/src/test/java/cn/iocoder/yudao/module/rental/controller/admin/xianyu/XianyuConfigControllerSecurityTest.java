package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigUpdateReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class XianyuConfigControllerSecurityTest {

    private static final String SUPER_ADMIN_ONLY = "@ss.hasRole('super_admin')";

    @Test
    void getConfigRetainsMaskedOperationalQueryPermission() throws NoSuchMethodException {
        Method method = XianyuConfigController.class.getDeclaredMethod("getConfig");
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("@ss.hasPermission('rental:xianyu:query')");
    }

    @Test
    void updateConfigRequiresSuperAdminRole() throws NoSuchMethodException {
        assertSuperAdminOnly(XianyuConfigController.class.getDeclaredMethod(
                "updateConfig", XianyuConfigUpdateReqVO.class));
    }

    private static void assertSuperAdminOnly(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(SUPER_ADMIN_ONLY);
    }

}
