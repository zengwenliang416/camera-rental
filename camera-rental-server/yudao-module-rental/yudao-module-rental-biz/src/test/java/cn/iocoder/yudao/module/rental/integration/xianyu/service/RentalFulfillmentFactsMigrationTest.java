package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RentalFulfillmentFactsMigrationTest {

    @Test
    void migrationAddsExpectedReturnInspectionAndSettlementFacts() throws IOException {
        String migration = Files.readString(findProductionMigration());

        assertThat(migration)
                .contains("ADD COLUMN `expected_send_back_date` date DEFAULT NULL")
                .contains("ADD COLUMN `settled_at` datetime DEFAULT NULL")
                .contains("ADD COLUMN `inspection_completed_at` datetime DEFAULT NULL")
                .contains("ADD COLUMN `inspection_result` varchar(16) DEFAULT NULL");
        assertThat(countOccurrences(migration, "ADD COLUMN `expected_send_back_date`"))
                .isEqualTo(2);
    }

    @Test
    void developmentCopyMatchesProductionMigration() throws IOException {
        assertThat(Files.readAllBytes(findDevelopmentMigration()))
                .isEqualTo(Files.readAllBytes(findProductionMigration()));
    }

    @Test
    void rollbackDropsOnlyFactsIntroducedByMigration054() throws IOException {
        String rollback = Files.readString(findRollback());

        assertThat(rollback)
                .contains("DROP COLUMN `inspection_result`")
                .contains("DROP COLUMN `inspection_completed_at`")
                .contains("DROP COLUMN `expected_send_back_date`")
                .contains("DROP COLUMN `settled_at`")
                .doesNotContain("DROP TABLE")
                .doesNotContain("DELETE FROM");
        assertThat(countOccurrences(rollback, "DROP COLUMN `expected_send_back_date`"))
                .isEqualTo(2);
    }

    private static int countOccurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private Path findProductionMigration() {
        return findFirst(List.of(
                Path.of("sql/mysql/migrations/20260831_054_rental_fulfillment_facts.sql"),
                Path.of("../../sql/mysql/migrations/20260831_054_rental_fulfillment_facts.sql"),
                Path.of("camera-rental-server/sql/mysql/migrations/20260831_054_rental_fulfillment_facts.sql")));
    }

    private Path findDevelopmentMigration() {
        return findFirst(List.of(
                Path.of("../openspec/changes/add-rental-configuration/development/migrations/"
                        + "20260831_054_rental_fulfillment_facts.sql"),
                Path.of("../../../openspec/changes/add-rental-configuration/development/migrations/"
                        + "20260831_054_rental_fulfillment_facts.sql"),
                Path.of("openspec/changes/add-rental-configuration/development/migrations/"
                        + "20260831_054_rental_fulfillment_facts.sql")));
    }

    private Path findRollback() {
        return findFirst(List.of(
                Path.of("../openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260831_054_rental_fulfillment_facts.sql"),
                Path.of("../../../openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260831_054_rental_fulfillment_facts.sql"),
                Path.of("openspec/changes/add-rental-configuration/development/migrations/"
                        + "rollback-20260831_054_rental_fulfillment_facts.sql")));
    }

    private Path findFirst(List<Path> candidates) {
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot locate migration asset from " + candidates));
    }

}
