package cn.iocoder.yudao.module.rental.integration.xianyu.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianyuPropertiesTest {

    @Test
    void shouldKeepIntegrationWritesAndJobsDisabledByDefault() {
        XianyuProperties properties = new XianyuProperties();

        assertEquals(XianyuProperties.IntegrationStatus.DISABLED, properties.getIntegrationStatus());
        assertFalse(properties.isWriteEnabled());
        assertFalse(properties.getJob().isEnabled());
        assertTrue(properties.isTenantConfigurationValid());
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
    void serverConfigMustNotContainXianyuEnvironmentCompatibility() throws Exception {
        String yaml = Files.readString(findServerRoot()
                .resolve("yudao-server/src/main/resources/application-local.yaml"));
        String sharedYaml = Files.readString(findServerRoot()
                .resolve("yudao-server/src/main/resources/application.yaml"));

        assertFalse(yaml.contains("XGJ_"));
        assertFalse(yaml.contains("xianyu:"));
        assertFalse(sharedYaml.contains("XGJ_"));
        assertFalse(sharedYaml.contains("xianyu:"));
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
