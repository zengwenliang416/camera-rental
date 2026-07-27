package cn.iocoder.yudao.module.rental.integration.xianyu.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianyuPropertiesTest {

    @Test
    void shouldKeepReadIntegrationDisabledAndWriteSwitchEnabledByDefault() {
        XianyuProperties properties = new XianyuProperties();

        assertEquals(XianyuProperties.IntegrationStatus.DISABLED, properties.getIntegrationStatus());
        assertTrue(properties.isWriteEnabled());
        assertTrue(properties.isTenantConfigurationValid());
        assertFalse(properties.getJob().isStartupSyncEnabled());
    }

    @Test
    void shouldRequireRuntimeCredentialsWhenEnabled() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);

        assertEquals(XianyuProperties.IntegrationStatus.MISSING_CREDENTIALS, properties.getIntegrationStatus());

        properties.setAppKey("runtime-app-key");
        properties.setAppSecret("runtime-app-secret");

        assertEquals(XianyuProperties.IntegrationStatus.READY, properties.getIntegrationStatus());
        assertFalse(properties.isTenantConfigurationValid());
        assertThrows(IllegalStateException.class, properties::requireTenantId);

        properties.setTenantId(42L);

        assertTrue(properties.isTenantConfigurationValid());
        assertEquals(42L, properties.requireTenantId());
    }

    @Test
    void shouldRejectMissingTenantThroughBeanValidationWhenEnabled() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertFalse(validator.validate(properties).isEmpty());

        properties.setTenantId(42L);

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void localConfigMustSupportInfraJobRegistrationEnvAlias() throws Exception {
        String yaml = Files.readString(findServerRoot()
                .resolve("yudao-server/src/main/resources/application-local.yaml"));

        assertTrue(yaml.contains("XGJ_JOB_REGISTER_INFRA_JOBS"));
        assertTrue(yaml.contains("XGJ_JOB_REGISTER_INFRA"));
        assertTrue(yaml.contains("XGJ_JOB_PRODUCT_CRON"));
        assertTrue(yaml.contains("XGJ_JOB_AFTER_SALE_CRON"));
    }

    private Path findServerRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("yudao-server/src/main/resources/application-local.yaml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("camera-rental-server root not found");
    }

}
