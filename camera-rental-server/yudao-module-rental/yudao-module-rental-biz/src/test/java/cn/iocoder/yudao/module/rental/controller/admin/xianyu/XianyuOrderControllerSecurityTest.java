package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class XianyuOrderControllerSecurityTest {

    @Test
    void shipmentRuleBindingRequiresConfigurationUpdatePermission() throws NoSuchMethodException {
        Method method = XianyuOrderController.class.getDeclaredMethod("ship", XianyuOrderShipReqVO.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value())
                .contains("@ss.hasPermission('rental:xianyu:ship')")
                .contains("#reqVO.bindProductRuleIfMissing != true")
                .contains("@ss.hasPermission('rental:configuration:update')");
    }

}
