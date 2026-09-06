package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalOrderConfirmOutboundReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RentalOrderManualControllerSecurityTest {

    @Test
    void createManualOrderRequiresOrderCreatePermission() throws NoSuchMethodException {
        Method method = RentalOrderManualController.class.getDeclaredMethod(
                "createManualOrder", RentalManualOrderCreateReqVO.class);

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("@ss.hasPermission('rental:order:create')");
    }

    @Test
    void confirmOutboundRequiresOrderCreatePermission() throws NoSuchMethodException {
        Method method = RentalOrderManualController.class.getDeclaredMethod(
                "confirmOutbound", RentalOrderConfirmOutboundReqVO.class);

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("@ss.hasPermission('rental:order:create')");
    }

    @Test
    void suggestCustomerRequiresOrderCreatePermission() throws NoSuchMethodException {
        Method method = RentalOrderManualController.class.getDeclaredMethod(
                "suggestCustomer", String.class);

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("@ss.hasPermission('rental:order:create')");
    }

}
