package cn.iocoder.yudao.module.rental.service.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RentalConfigurationBackendMigrationTest {

    @Test
    void backendMigrationAddsCatalogOptimisticVersions() throws IOException {
        String migration = Files.readString(find(
                "sql/mysql/migrations/20260831_053_rental_configuration_backend.sql"));

        assertThat(migration)
                .contains("ALTER TABLE `rental_device_category`")
                .contains("ADD COLUMN `lock_version` int NOT NULL DEFAULT 0")
                .contains("ALTER TABLE `rental_device_model`");
    }

    @Test
    void controlledSeedRequiresUniqueExactAuthorizedShopResolution() throws IOException {
        String seed = Files.readString(find(
                "sql/mysql/seeds/20260831_rental_configuration_skipped_items.sql"));

        assertThat(seed)
                .contains("TRIM(`shop_name`) = '小疆同学'")
                .contains("TRIM(`shop_name`) = '发发学长'")
                .contains("`authorization_status` = 'VALID'")
                .contains("SIGNAL SQLSTATE '45000'")
                .contains("'CONFIG_SKIPPED'")
                .contains("'NONE'")
                .doesNotContain("LIKE '%小疆%'")
                .doesNotContain("LIKE '%发发%'");
    }

    @Test
    void controlledSeedContainsExactlyTheApproved29ItemIds() throws IOException {
        String seed = Files.readString(find(
                "sql/mysql/seeds/20260831_rental_configuration_skipped_items.sql"));
        Matcher matcher = Pattern.compile("'([0-9]{12,})'").matcher(seed);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertThat(count).isEqualTo(29);
    }

    private Path find(String relative) {
        List<Path> candidates = List.of(
                Path.of(relative),
                Path.of("../../" + relative),
                Path.of("camera-rental-server/" + relative.substring("sql/".length())));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate " + relative));
    }
}
