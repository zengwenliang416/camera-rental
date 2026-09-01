package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RentalHistoricalReconciliationMigrationTest {

    @Test
    void migrationCreatesDurableRunAndFailureTables() throws IOException {
        String migration = Files.readString(findProductionMigration());

        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS `rental_historical_reconciliation_run`")
                .contains("CREATE TABLE IF NOT EXISTS `rental_historical_reconciliation_failure`")
                .contains("`cursor_after_id` bigint NOT NULL DEFAULT 0")
                .contains("`scanned_count` int NOT NULL DEFAULT 0")
                .contains("`review_required_count` int NOT NULL DEFAULT 0")
                .contains("`last_failed_order_id` bigint DEFAULT NULL")
                .contains("`execution_token` varchar(64) DEFAULT NULL")
                .contains("`lease_until` datetime DEFAULT NULL")
                .contains("`heartbeat_at` datetime DEFAULT NULL")
                .contains("KEY `idx_rental_history_run_status`")
                .contains("KEY `idx_rental_history_run_lease`")
                .contains("KEY `idx_rental_history_failure_run`");
    }

    @Test
    void developmentCopyMatchesProductionMigration() throws IOException {
        assertThat(Files.readAllBytes(findDevelopmentMigration()))
                .isEqualTo(Files.readAllBytes(findProductionMigration()));
    }

    @Test
    void rollbackDropsOnlyMigration055Tables() throws IOException {
        String rollback = Files.readString(findRollback());

        assertThat(rollback)
                .contains("DROP TABLE IF EXISTS `rental_historical_reconciliation_failure`")
                .contains("DROP TABLE IF EXISTS `rental_historical_reconciliation_run`")
                .doesNotContain("DROP TABLE IF EXISTS `xianyu_order`")
                .doesNotContain("DELETE FROM")
                .doesNotContain("UPDATE ");
    }

    private Path findProductionMigration() {
        return findFirst(List.of(
                Path.of("sql/mysql/migrations/20260901_055_rental_historical_reconciliation.sql"),
                Path.of("../../sql/mysql/migrations/20260901_055_rental_historical_reconciliation.sql"),
                Path.of("camera-rental-server/sql/mysql/migrations/"
                        + "20260901_055_rental_historical_reconciliation.sql")));
    }

    private Path findDevelopmentMigration() {
        return findFirst(List.of(
                Path.of("../openspec/changes/add-rental-configuration/development/migrations/"
                        + "20260901_055_rental_historical_reconciliation.sql"),
                Path.of("../../../openspec/changes/add-rental-configuration/development/migrations/"
                        + "20260901_055_rental_historical_reconciliation.sql"),
                Path.of("openspec/changes/add-rental-configuration/development/migrations/"
                        + "20260901_055_rental_historical_reconciliation.sql")));
    }

    private Path findRollback() {
        return findFirst(List.of(
                Path.of("../openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260901_055_rental_historical_reconciliation.sql"),
                Path.of("../../../openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260901_055_rental_historical_reconciliation.sql"),
                Path.of("openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260901_055_rental_historical_reconciliation.sql")));
    }

    private Path findFirst(List<Path> candidates) {
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot locate migration asset from " + candidates));
    }

}
