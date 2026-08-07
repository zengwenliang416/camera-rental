package cn.iocoder.yudao.module.rental.controller.admin.logistics.operations;

import cn.iocoder.yudao.module.rental.controller.admin.logistics.RentalDeliveryTrackingController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RentalLogisticsOperationsPermissionMigrationTest {

    private static final Pattern PERMISSION =
            Pattern.compile("hasPermission\\('([^']+)'\\)");

    @Test
    void everyLogisticsControllerPermissionIsGrantableFromMigration() throws IOException {
        String migration = Files.readString(findMigration());
        Set<String> permissions = new LinkedHashSet<>();
        collectPermissions(RentalDeliveryTrackingController.class, permissions);
        collectPermissions(RentalLogisticsOperationsController.class, permissions);

        assertEquals(9, permissions.size());
        for (String permission : permissions) {
            assertTrue(migration.contains("'" + permission + "'"),
                    () -> "Missing system_menu permission: " + permission);
        }
        assertEquals(13, countOccurrences(migration, "'rental:"));
        assertTrue(migration.contains("permission_row.`sort`, 7081"));
        assertFalse(migration.contains("INSERT INTO `system_role_menu`"));
    }

    private void collectPermissions(Class<?> controllerType, Set<String> permissions) {
        for (Method method : controllerType.getDeclaredMethods()) {
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            if (annotation == null) {
                continue;
            }
            Matcher matcher = PERMISSION.matcher(annotation.value());
            if (matcher.find()) {
                permissions.add(matcher.group(1));
            }
        }
    }

    private int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private Path findMigration() {
        List<Path> candidates = List.of(
                Path.of("sql/mysql/migrations/20260731_032_rental_delivery_tracking.sql"),
                Path.of("../../sql/mysql/migrations/20260731_032_rental_delivery_tracking.sql"),
                Path.of("camera-rental-server/sql/mysql/migrations/20260731_032_rental_delivery_tracking.sql"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate logistics migration"));
    }
}
