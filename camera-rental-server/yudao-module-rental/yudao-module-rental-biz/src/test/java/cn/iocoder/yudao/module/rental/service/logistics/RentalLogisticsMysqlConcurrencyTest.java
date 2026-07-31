package cn.iocoder.yudao.module.rental.service.logistics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RentalLogisticsMysqlConcurrencyTest {

    private static final String JDBC_URL_ENV = "RENTAL_LOGISTICS_MYSQL_JDBC_URL";
    private static final String USER_ENV = "RENTAL_LOGISTICS_MYSQL_USER";
    private static final String PASSWORD_ENV = "RENTAL_LOGISTICS_MYSQL_PASSWORD";

    private String jdbcUrl;
    private String user;
    private String password;

    @BeforeEach
    void setUp() throws SQLException {
        jdbcUrl = System.getenv(JDBC_URL_ENV);
        user = System.getenv(USER_ENV);
        password = System.getenv(PASSWORD_ENV);
        assumeTrue(jdbcUrl != null && !jdbcUrl.isBlank(),
                JDBC_URL_ENV + " is required for MySQL concurrency verification");
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM rental_delivery_callback_inbox");
            statement.executeUpdate("DELETE FROM rental_delivery_outbox");
            statement.executeUpdate("DELETE FROM rental_delivery");
        }
    }

    @AfterEach
    void clearRows() throws SQLException {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM rental_delivery_callback_inbox");
            statement.executeUpdate("DELETE FROM rental_delivery_outbox");
            statement.executeUpdate("DELETE FROM rental_delivery");
        }
    }

    @Test
    void sameDeliveryAndPayloadConcurrentlyReuseOneInbox() throws Exception {
        Set<Long> ids = runConcurrently(
                () -> insertOrReuseInbox(9L, 10L, "same-payload"),
                () -> insertOrReuseInbox(9L, 10L, "same-payload"));

        assertEquals(1, ids.size());
        assertEquals(1, count("rental_delivery_callback_inbox"));
    }

    @Test
    void differentDeliveriesWithSamePayloadPersistSeparately() throws Exception {
        Set<Long> ids = runConcurrently(
                () -> insertOrReuseInbox(9L, 10L, "same-payload"),
                () -> insertOrReuseInbox(9L, 11L, "same-payload"));

        assertEquals(2, ids.size());
        assertEquals(2, count("rental_delivery_callback_inbox"));
    }

    @Test
    void sameOutboxDedupeKeyConcurrentlyReusesOneTask() throws Exception {
        Set<Long> ids = runConcurrently(
                () -> insertOrReuseOutbox(9L, 10L, "delivery:10:SUBSCRIBE"),
                () -> insertOrReuseOutbox(9L, 10L, "delivery:10:SUBSCRIBE"));

        assertEquals(1, ids.size());
        assertEquals(1, count("rental_delivery_outbox"));
    }

    @Test
    void skipLockedAllowsSecondWorkerToLeaseAnotherTask() throws Exception {
        insertOrReuseOutbox(9L, 10L, "delivery:10:INITIAL_QUERY");
        insertOrReuseOutbox(9L, 11L, "delivery:11:INITIAL_QUERY");
        try (Connection first = connection(); Connection second = connection()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            Long firstId = selectClaimableOutbox(first);
            Long secondId = selectClaimableOutbox(second);
            assertNotEquals(firstId, secondId);
            first.commit();
            second.commit();
        }
    }

    @Test
    void callbackTokenHashCanRepeatAcrossTenants() throws SQLException {
        insertDelivery(1L, 100L, "source-1", "WB-1", "callback-hash");
        insertDelivery(2L, 200L, "source-2", "WB-2", "callback-hash");

        assertEquals(2, countByCallbackTokenHash("callback-hash"));
    }

    @Test
    void callbackTokenHashRemainsUniqueWithinTenant() throws SQLException {
        insertDelivery(1L, 100L, "source-1", "WB-1", "callback-hash");
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> insertDelivery(1L, 200L, "source-2", "WB-2", "callback-hash"));
    }

    private Set<Long> runConcurrently(Callable<Long> firstTask, Callable<Long> secondTask) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Long> first = executor.submit(awaitStart(firstTask, ready, start));
            Future<Long> second = executor.submit(awaitStart(secondTask, ready, start));
            ready.await();
            start.countDown();
            Set<Long> ids = new HashSet<>();
            ids.add(first.get());
            ids.add(second.get());
            return ids;
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Long> awaitStart(Callable<Long> task, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return task.call();
        };
    }

    private Long insertOrReuseInbox(Long tenantId, Long deliveryId, String payloadHash) throws SQLException {
        String insertSql = """
                INSERT INTO rental_delivery_callback_inbox (
                    tenant_id, provider_code, delivery_id, payload_hash, callback_params,
                    processing_status, retry_count, received_at
                ) VALUES (?, 'KUAIDI100', ?, ?, 'encrypted-fixture', 'RECEIVED', 0, NOW())
                ON DUPLICATE KEY UPDATE id = id
                """;
        String selectSql = """
                SELECT id
                FROM rental_delivery_callback_inbox
                WHERE tenant_id = ?
                  AND provider_code = 'KUAIDI100'
                  AND delivery_id = ?
                  AND payload_hash = ?
                FOR UPDATE
                """;
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setLong(1, tenantId);
                insert.setLong(2, deliveryId);
                insert.setString(3, payloadHash);
                insert.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setLong(1, tenantId);
                select.setLong(2, deliveryId);
                select.setString(3, payloadHash);
                try (ResultSet result = select.executeQuery()) {
                    result.next();
                    Long id = result.getLong(1);
                    connection.commit();
                    return id;
                }
            }
        }
    }

    private Long insertOrReuseOutbox(Long tenantId, Long deliveryId, String dedupeKey) throws SQLException {
        String insertSql = """
                INSERT INTO rental_delivery_outbox (
                    tenant_id, delivery_id, event_type, dedupe_key, processing_status,
                    retry_count, scheduled_at
                ) VALUES (?, ?, 'INITIAL_QUERY', ?, 'PENDING', 0, NOW())
                ON DUPLICATE KEY UPDATE id = id
                """;
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setLong(1, tenantId);
                insert.setLong(2, deliveryId);
                insert.setString(3, dedupeKey);
                insert.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id FROM rental_delivery_outbox WHERE tenant_id = ? AND dedupe_key = ? FOR UPDATE")) {
                select.setLong(1, tenantId);
                select.setString(2, dedupeKey);
                try (ResultSet result = select.executeQuery()) {
                    result.next();
                    Long id = result.getLong(1);
                    connection.commit();
                    return id;
                }
            }
        }
    }

    private Long selectClaimableOutbox(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id
                FROM rental_delivery_outbox
                WHERE tenant_id = 9
                  AND deleted = b'0'
                  AND (
                    (processing_status IN ('PENDING', 'RETRY_WAIT')
                      AND (next_attempt_at IS NULL OR next_attempt_at <= NOW()))
                    OR (processing_status = 'PROCESSING' AND lease_until < NOW())
                  )
                ORDER BY id
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    private void insertDelivery(Long tenantId, Long rentalOrderId, String sourceIdentifier,
                                String waybillNo, String callbackTokenHash) throws SQLException {
        String sql = """
                INSERT INTO rental_delivery (
                    tenant_id, rental_order_id, direction, source_type, source_identifier,
                    source_carrier_code, canonical_carrier_code, waybill_no,
                    normalized_waybill_no, callback_token_hash
                ) VALUES (?, ?, 'OUTBOUND', 'TEST', ?, 'SF', 'SF', ?, ?, ?)
                """;
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tenantId);
            statement.setLong(2, rentalOrderId);
            statement.setString(3, sourceIdentifier);
            statement.setString(4, waybillNo);
            statement.setString(5, waybillNo);
            statement.setString(6, callbackTokenHash);
            statement.executeUpdate();
        }
    }

    private int count(String table) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private int countByCallbackTokenHash(String callbackTokenHash) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM rental_delivery WHERE callback_token_hash = ?")) {
            statement.setString(1, callbackTokenHash);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }
}
