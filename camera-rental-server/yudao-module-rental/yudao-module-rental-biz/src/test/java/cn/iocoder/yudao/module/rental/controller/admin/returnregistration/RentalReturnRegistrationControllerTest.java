package cn.iocoder.yudao.module.rental.controller.admin.returnregistration;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RentalReturnRegistrationControllerTest {

    @Test
    void everyAdminOperationHasAnExplicitPermission() throws Exception {
        assertPermission("get", new Class<?>[]{Long.class},
                "rental:return-registration:query");
        assertPermission("revoke", new Class<?>[]{Long.class},
                "rental:return-registration:revoke");
        assertPermission("review",
                new Class<?>[]{Long.class, RentalReturnRegistrationController.ReviewReq.class},
                "rental:return-registration:review");
    }

    @Test
    void manualTokenIssueOperationsAreNotExposed() {
        assertFalse(Arrays.stream(RentalReturnRegistrationController.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("create")
                        || method.getName().equals("reissue")));
    }

    private void assertPermission(String method, Class<?>[] parameterTypes,
                                  String permission) throws Exception {
        PreAuthorize annotation = RentalReturnRegistrationController.class
                .getDeclaredMethod(method, parameterTypes)
                .getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('" + permission + "')", annotation.value());
    }
}
