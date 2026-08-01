package cn.iocoder.yudao.module.rental.controller.admin.returnregistration;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RentalReturnRegistrationControllerTest {

    @Test
    void everyAdminOperationHasAnExplicitPermission() throws Exception {
        assertPermission("create",
                new Class<?>[]{RentalReturnRegistrationController.CreateReq.class},
                "rental:return-registration:create");
        assertPermission("reissue",
                new Class<?>[]{Long.class, RentalReturnRegistrationController.ReissueReq.class},
                "rental:return-registration:create");
        assertPermission("get", new Class<?>[]{Long.class},
                "rental:return-registration:query");
        assertPermission("revoke", new Class<?>[]{Long.class},
                "rental:return-registration:revoke");
        assertPermission("review",
                new Class<?>[]{Long.class, RentalReturnRegistrationController.ReviewReq.class},
                "rental:return-registration:review");
    }

    private void assertPermission(String method, Class<?>[] parameterTypes,
                                  String permission) throws Exception {
        PreAuthorize annotation = RentalReturnRegistrationController.class
                .getDeclaredMethod(method, parameterTypes)
                .getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('" + permission + "')", annotation.value());
    }
}
