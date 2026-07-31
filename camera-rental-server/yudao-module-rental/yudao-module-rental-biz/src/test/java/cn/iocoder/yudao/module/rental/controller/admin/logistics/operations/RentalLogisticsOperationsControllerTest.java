package cn.iocoder.yudao.module.rental.controller.admin.logistics.operations;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentalLogisticsOperationsControllerTest {

    @Test
    void everyOperationHasItsExplicitBackendPermission() throws Exception {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("getProviderConfig", "rental:logistics:config:query"),
                Map.entry("saveProviderConfig", "rental:logistics:config:update"),
                Map.entry("verifyProviderConfig", "rental:logistics:config:verify"),
                Map.entry("saveProviderCredential", "rental:logistics:config:update"),
                Map.entry("deleteProviderCredential", "rental:logistics:config:update"),
                Map.entry("verifyProviderCredential", "rental:logistics:config:verify"),
                Map.entry("listCarrierMappings", "rental:logistics:mapping:query"),
                Map.entry("saveCarrierMapping", "rental:logistics:mapping:update"),
                Map.entry("deleteCarrierMapping", "rental:logistics:mapping:delete"),
                Map.entry("listFailedTasks", "rental:logistics:task:query"),
                Map.entry("retryFailedTask", "rental:logistics:task:retry"),
                Map.entry("reconcile", "rental:logistics:reconcile"),
                Map.entry("getMetrics", "rental:logistics:metrics:query"),
                Map.entry("backfill", "rental:logistics:backfill"),
                Map.entry("cleanup", "rental:logistics:cleanup"));

        for (Method method : RentalLogisticsOperationsController.class.getDeclaredMethods()) {
            if (!expected.containsKey(method.getName())) {
                continue;
            }
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertNotNull(annotation, method.getName());
            assertEquals("@ss.hasPermission('" + expected.get(method.getName()) + "')", annotation.value());
        }
        assertEquals(expected.size(), RentalLogisticsOperationsController.class.getDeclaredMethods().length);
    }
}
