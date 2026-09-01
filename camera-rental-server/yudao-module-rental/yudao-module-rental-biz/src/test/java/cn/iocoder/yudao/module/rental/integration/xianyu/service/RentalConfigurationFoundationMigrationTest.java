package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RentalConfigurationFoundationMigrationTest {

    @Test
    void migrationCreatesExplicitIdentifierAndConfigurationFoundation() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .contains("ADD COLUMN `xianyu_user_name`")
                .contains("MODIFY COLUMN `external_product_id` varchar(128) DEFAULT NULL")
                .contains("MODIFY COLUMN `external_sku_id` varchar(128) DEFAULT NULL")
                .contains("ADD COLUMN `xgj_product_id`")
                .contains("ADD COLUMN `xianyu_item_id`")
                .contains("ADD COLUMN `xgj_sku_id`")
                .contains("ADD COLUMN `xianyu_sku_id`")
                .contains("CREATE TABLE IF NOT EXISTS `rental_channel_product_rule`")
                .contains("CREATE TABLE IF NOT EXISTS `rental_channel_product_sku_mapping`")
                .contains("CREATE TABLE IF NOT EXISTS `xianyu_order_remark_history`")
                .contains("'rental:configuration:query'")
                .contains("'rental:configuration:update'");
    }

    @Test
    void orderBackfillUsesOnlyExplicitGoodsJsonAndLeavesAmbiguityUnmapped() throws IOException {
        String migration = Files.readString(findMigration());

        assertThat(migration)
                .contains("JSON_EXTRACT(`goods_json`, '$.product_id')")
                .contains("JSON_EXTRACT(`goods_json`, '$.item_id')")
                .contains("JSON_EXTRACT(`goods_json`, '$.sku_id')")
                .contains("HAVING COUNT(DISTINCT `xgj_product_id`) = 1")
                .contains("HAVING COUNT(DISTINCT by_product.`xianyu_item_id`) = 1")
                .doesNotContain("SET `xgj_product_id` = `external_product_id`")
                .doesNotContain("SET `xianyu_item_id` = `external_product_id`")
                .doesNotContain("SET `xgj_sku_id` = `external_sku_id`");
    }

    @Test
    void rollbackRestoresRequiredLegacyValuesBeforeDroppingExplicitIdentifiers() throws IOException {
        String rollback = Files.readString(findRollback());

        assertThat(rollback)
                .contains("SET `external_product_id` = `xgj_product_id`")
                .contains("WHERE `external_product_id` IS NULL")
                .contains("SET `external_sku_id` = `xgj_sku_id`")
                .contains("WHERE `external_sku_id` IS NULL");
        assertThat(rollback.indexOf("SET `external_product_id` = `xgj_product_id`"))
                .isLessThan(rollback.indexOf("MODIFY COLUMN `external_product_id` varchar(128) NOT NULL"));
        assertThat(rollback.indexOf("SET `external_sku_id` = `xgj_sku_id`"))
                .isLessThan(rollback.indexOf("MODIFY COLUMN `external_sku_id` varchar(128) NOT NULL"));
    }

    private Path findMigration() {
        List<Path> candidates = List.of(
                Path.of("sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql"),
                Path.of("../../sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql"),
                Path.of("camera-rental-server/sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate rental configuration migration"));
    }

    private Path findRollback() {
        List<Path> candidates = List.of(
                Path.of("../openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260831_052_rental_configuration_foundation.sql"),
                Path.of("../../../openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260831_052_rental_configuration_foundation.sql"),
                Path.of("openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260831_052_rental_configuration_foundation.sql"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate rental configuration rollback"));
    }

}
