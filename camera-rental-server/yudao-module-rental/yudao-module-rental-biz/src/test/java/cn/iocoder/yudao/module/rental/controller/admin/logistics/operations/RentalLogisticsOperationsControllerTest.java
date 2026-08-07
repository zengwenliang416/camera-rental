package cn.iocoder.yudao.module.rental.controller.admin.logistics.operations;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentalLogisticsOperationsControllerTest {

    private static final String SUPER_ADMIN_ONLY = "@ss.hasRole('super_admin')";

    @Test
    void everyOperationHasItsExplicitBackendPermission() throws Exception {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("getProviderConfig", SUPER_ADMIN_ONLY),
                Map.entry("saveProviderConfig", SUPER_ADMIN_ONLY),
                Map.entry("verifyProviderConfig", SUPER_ADMIN_ONLY),
                Map.entry("saveProviderCredential", SUPER_ADMIN_ONLY),
                Map.entry("deleteProviderCredential", SUPER_ADMIN_ONLY),
                Map.entry("verifyProviderCredential", SUPER_ADMIN_ONLY),
                Map.entry("listCarrierMappings", permission("rental:logistics:mapping:query")),
                Map.entry("saveCarrierMapping", permission("rental:logistics:mapping:update")),
                Map.entry("deleteCarrierMapping", permission("rental:logistics:mapping:delete")),
                Map.entry("listFailedTasks", permission("rental:logistics:task:query")),
                Map.entry("retryFailedTask", permission("rental:logistics:task:retry")),
                Map.entry("reconcile", permission("rental:logistics:reconcile")),
                Map.entry("getMetrics", permission("rental:logistics:metrics:query")),
                Map.entry("backfill", SUPER_ADMIN_ONLY),
                Map.entry("cleanup", permission("rental:logistics:cleanup")));

        for (Method method : RentalLogisticsOperationsController.class.getDeclaredMethods()) {
            if (!expected.containsKey(method.getName())) {
                continue;
            }
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertNotNull(annotation, method.getName());
            assertEquals(expected.get(method.getName()), annotation.value());
        }
        assertEquals(expected.size(), RentalLogisticsOperationsController.class.getDeclaredMethods().length);
    }

    private static String permission(String permission) {
        return "@ss.hasPermission('" + permission + "')";
    }
}
