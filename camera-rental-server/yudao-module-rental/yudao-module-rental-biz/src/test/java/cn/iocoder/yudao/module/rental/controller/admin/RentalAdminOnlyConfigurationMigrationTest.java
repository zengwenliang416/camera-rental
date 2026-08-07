package cn.iocoder.yudao.module.rental.controller.admin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RentalAdminOnlyConfigurationMigrationTest {

    @Test
    void configurationPagesStayUnderRentalOperationsButLoseOrdinaryRoleGrants() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .contains("`parent_id` = 7000")
                .contains("`path` = 'xianyu'")
                .contains("`path` = 'logistics/config'")
                .contains("`menu_id` IN (7001, 7004, 7005, 7072, 7083, 7084, 7085, 7095)")
                .contains("restricted_menu.`permission` = 'rental:logistics:backfill'")
                .doesNotContain("`parent_id` = 1")
                .doesNotContain("xianyu-integration");
    }

    @Test
    void employeeOrderOperationsAreNotRevoked() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .doesNotContain("7002")
                .doesNotContain("7003")
                .doesNotContain("7070")
                .doesNotContain("7071");
    }

    private Path findMigration() {
        List<Path> candidates = List.of(
                Path.of("sql/mysql/migrations/20260807_044_xianyu_admin_only_console.sql"),
                Path.of("../../sql/mysql/migrations/20260807_044_xianyu_admin_only_console.sql"),
                Path.of("camera-rental-server/sql/mysql/migrations/20260807_044_xianyu_admin_only_console.sql"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate admin-only configuration migration"));
    }

}
