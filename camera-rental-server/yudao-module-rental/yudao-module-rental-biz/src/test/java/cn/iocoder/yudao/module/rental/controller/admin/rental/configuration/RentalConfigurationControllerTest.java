package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentalConfigurationControllerTest {

    @Test
    void controllerUsesStandaloneConfigurationRoute() {
        RequestMapping mapping = RentalConfigurationController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/rental/configuration", mapping.value()[0]);
    }

    @Test
    void everyEndpointUsesConfigurationQueryOrUpdatePermission() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("getCatalog", permission("rental:configuration:query")),
                Map.entry("getConfigurationShops", permission("rental:configuration:query")),
                Map.entry("createCategory", permission("rental:configuration:update")),
                Map.entry("updateCategory", permission("rental:configuration:update")),
                Map.entry("updateCategoryStatus", permission("rental:configuration:update")),
                Map.entry("createModel", permission("rental:configuration:update")),
                Map.entry("updateModel", permission("rental:configuration:update")),
                Map.entry("updateModelStatus", permission("rental:configuration:update")),
                Map.entry("getProductRulePage", permission("rental:configuration:query")),
                Map.entry("getProductRule", permission("rental:configuration:query")),
                Map.entry("getSyncedSkus", permission("rental:configuration:query")),
                Map.entry("previewProductRuleImpact", permission("rental:configuration:query")),
                Map.entry("createProductRule", permission("rental:configuration:update")),
                Map.entry("updateProductRule", permission("rental:configuration:update")),
                Map.entry("updateProductRuleStatus", permission("rental:configuration:update")),
                Map.entry("getProductRuleReconciliation", permission("rental:configuration:query")),
                Map.entry("runHistoricalReconciliation", permission("rental:configuration:update")),
                Map.entry("getHistoricalReconciliation", permission("rental:configuration:query")),
                Map.entry("pauseHistoricalReconciliation", permission("rental:configuration:update")),
                Map.entry("resumeHistoricalReconciliation", permission("rental:configuration:update")));

        for (Method method : RentalConfigurationController.class.getDeclaredMethods()) {
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertNotNull(annotation, method.getName());
            assertEquals(expected.get(method.getName()), annotation.value(), method.getName());
        }
        assertEquals(expected.size(), RentalConfigurationController.class.getDeclaredMethods().length);
    }

    private static String permission(String permission) {
        return "@ss.hasPermission('" + permission + "')";
    }
}
