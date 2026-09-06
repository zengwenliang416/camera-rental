package cn.iocoder.yudao.module.rental.service.rental;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RentalOfflineManualOrderMigrationTest {

    private static final String MIGRATION_NAME = "20260906_058_rental_offline_manual_order.sql";

    @Test
    void migrationAddsOrderColumnsAndNewTables() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .contains("ALTER TABLE `rental_order`")
                .contains("ADD COLUMN `customer_id` bigint DEFAULT NULL")
                .contains("ADD COLUMN `deposit_amount` bigint DEFAULT NULL")
                .contains("押金，单位分")
                .contains("CREATE TABLE IF NOT EXISTS `rental_customer`")
                .contains("CREATE TABLE IF NOT EXISTS `rental_order_delivery`")
                .contains("`mobile` varchar(255) NOT NULL")
                .contains("`receiver_mobile` varchar(255) DEFAULT NULL")
                .contains("`receiver_address` varchar(512) DEFAULT NULL")
                .contains("UNIQUE KEY `uk_rental_order_delivery_order` (`tenant_id`, `rental_order_id`)")
                .contains("KEY `idx_rental_customer_mobile` (`tenant_id`, `mobile`)");
    }

    @Test
    void migrationAddsMenuAndButtonIdempotently() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .contains("7113, '线下录单', '', 2, 6, 7000")
                .contains("'rental/order-create/index', 'RentalOrderCreate'")
                .contains("7114, '线下录单提交', 'rental:order:create', 3, 1, 7113")
                .contains("FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7113)")
                .contains("FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7114)");
        // Role grant mirrors 20260807_045: idempotent NOT EXISTS guard, based on menu 7010.
        assertThat(migration)
                .contains("INSERT INTO `system_role_menu`")
                .contains("order_role.`menu_id` = 7010")
                .contains("NOT EXISTS");
    }

    @Test
    void migrationIsRegisteredForDeployment() throws IOException {
        Path registry = findFirst(List.of(
                Path.of("ops/github-deploy/migrations.txt"),
                Path.of("../../ops/github-deploy/migrations.txt"),
                Path.of("../../../ops/github-deploy/migrations.txt")));

        assertThat(Files.readString(registry))
                .contains("camera-rental-server/sql/mysql/migrations/" + MIGRATION_NAME);
    }

    private Path findMigration() {
        return findFirst(List.of(
                Path.of("sql/mysql/migrations/" + MIGRATION_NAME),
                Path.of("../../sql/mysql/migrations/" + MIGRATION_NAME),
                Path.of("camera-rental-server/sql/mysql/migrations/" + MIGRATION_NAME)));
    }

    private Path findFirst(List<Path> candidates) {
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot locate migration asset from " + candidates));
    }

}
