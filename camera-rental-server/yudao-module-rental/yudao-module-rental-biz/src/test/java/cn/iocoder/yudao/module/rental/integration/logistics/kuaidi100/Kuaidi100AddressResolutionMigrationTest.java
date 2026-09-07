package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Kuaidi100AddressResolutionMigrationTest {

    private static final String MIGRATION_NAME = "20260907_059_kuaidi100_address_resolution.sql";

    @Test
    void migrationAddsAddressCapabilityAndEncryptedSecretColumns() throws IOException {
        String migration = Files.readString(findFirst(List.of(
                Path.of("sql/mysql/migrations/" + MIGRATION_NAME),
                Path.of("../../sql/mysql/migrations/" + MIGRATION_NAME),
                Path.of("camera-rental-server/sql/mysql/migrations/" + MIGRATION_NAME))));

        assertThat(migration)
                .contains("ADD COLUMN `address_parse_enabled` bit(1) NOT NULL DEFAULT b'0'")
                .contains("ADD COLUMN `api_secret` varchar(512) NULL")
                .doesNotContain("fixture-secret");
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

    private Path findFirst(List<Path> candidates) {
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate migration asset from " + candidates));
    }
}
