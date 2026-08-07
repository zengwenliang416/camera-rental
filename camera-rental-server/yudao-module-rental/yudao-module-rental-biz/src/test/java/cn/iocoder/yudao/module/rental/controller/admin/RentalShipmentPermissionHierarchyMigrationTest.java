package cn.iocoder.yudao.module.rental.controller.admin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RentalShipmentPermissionHierarchyMigrationTest {

    @Test
    void shipmentPermissionsBelongToChannelOrdersAndAreGrantedFromOrderAccess() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .contains("WHERE `id` = 7070")
                .contains("WHERE `id` = 7071")
                .contains("`parent_id` = 7010")
                .contains("order_role.`menu_id` = 7010")
                .contains("SELECT 7070 AS `menu_id`")
                .contains("UNION ALL SELECT 7071")
                .doesNotContain("order_role.`menu_id` = 7001");
    }

    @Test
    void migrationDoesNotRestoreConfigurationPageGrants() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .doesNotContain("`menu_id` = 7001")
                .doesNotContain("`menu_id` = 7072")
                .doesNotContain("`menu_id` = 7095");
    }

    private Path findMigration() {
        List<Path> candidates = List.of(
                Path.of("sql/mysql/migrations/20260807_045_xianyu_ship_permissions_under_orders.sql"),
                Path.of("../../sql/mysql/migrations/20260807_045_xianyu_ship_permissions_under_orders.sql"),
                Path.of("camera-rental-server/sql/mysql/migrations/20260807_045_xianyu_ship_permissions_under_orders.sql"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate shipment permission migration"));
    }

}
