package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalChannelReconciliationRunMigrationTest {

    @Test
    void migrationDefinesTenantScopedQueryableRunLedger() throws Exception {
        Path production = findRepositoryRoot().resolve(
                "camera-rental-server/sql/mysql/migrations/"
                        + "20260901_056_rental_channel_reconciliation_run.sql");
        Path reviewCopy = findRepositoryRoot().resolve(
                "openspec/changes/add-rental-configuration/development/migrations/"
                        + "20260901_056_rental_channel_reconciliation_run.sql");
        String sql = Files.readString(production);

        assertEquals(Files.readString(production), Files.readString(reviewCopy));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `rental_channel_reconciliation_run`"));
        for (String column : new String[]{
                "`tenant_id`", "`product_rule_id`", "`shop_id`", "`xianyu_item_id`",
                "`status`", "`scanned_count`", "`skipped_count`", "`created_count`",
                "`updated_count`", "`unchanged_count`", "`conflict_count`",
                "`failed_count`", "`review_required_count`", "`last_error_code`",
                "`started_at`", "`finished_at`"}) {
            assertTrue(sql.contains(column), column);
        }
        assertTrue(sql.contains("`tenant_id`, `status`, `create_time`"));
        assertTrue(sql.contains("`tenant_id`, `product_rule_id`, `id`"));
        assertTrue(sql.contains("`tenant_id`, `shop_id`, `xianyu_item_id`, `id`"));
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("camera-rental-server"))
                    && Files.isDirectory(current.resolve("openspec"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root not found");
    }

}
