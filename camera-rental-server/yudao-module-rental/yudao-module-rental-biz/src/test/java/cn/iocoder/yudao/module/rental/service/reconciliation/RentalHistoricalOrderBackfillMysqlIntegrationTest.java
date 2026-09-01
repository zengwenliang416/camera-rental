package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "RENTAL_HISTORICAL_MYSQL_JDBC_URL", matches = ".+")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = BaseDbUnitTest.Application.class,
        properties = {
                "spring.main.lazy-initialization=true",
                "spring.sql.init.mode=never",
                "mybatis.lazy-initialization=true",
                "yudao.info.base-package=cn.iocoder.yudao.module.rental.dal.mysql",
                "rental.historical-backfill.write-enabled=true"
        })
@Import({
        RentalHistoricalOrderBackfillService.class,
        RentalChannelOrderReconciliationService.class,
        RentalFulfillmentUpdateGuard.class,
        RentalOrderPreparationPolicy.class,
        RentalChannelOrderEligibilityPolicy.class,
        RentalRemarkPlanChangeClassifier.class,
        RentalDeviceLockService.class,
        RentalHistoricalOrderBackfillMysqlIntegrationTest.ClockConfiguration.class
})
class RentalHistoricalOrderBackfillMysqlIntegrationTest {

    private static final long TENANT_ID = 9L;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> System.getenv("RENTAL_HISTORICAL_MYSQL_JDBC_URL"));
        registry.add("spring.datasource.username",
                () -> System.getenv("RENTAL_HISTORICAL_MYSQL_USER"));
        registry.add("spring.datasource.password",
                () -> System.getenv("RENTAL_HISTORICAL_MYSQL_PASSWORD"));
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
    }

    @Resource
    private RentalHistoricalOrderBackfillService service;
    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        jdbcTemplate = new JdbcTemplate(dataSource);
        resetFixture();
        insertFixture();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void realServiceCreatesSkipsProtectsFulfillmentAndRerunsIdempotently() {
        RentalHistoricalBackfillRunResult first = service.createAndRun(
                new RentalHistoricalBackfillCommand(
                        100, 103, 10, 1, false,
                        RentalHistoricalOrderBackfillService.WRITE_CONFIRMATION));

        assertEquals("SUCCEEDED", first.status());
        assertEquals(3, first.scannedCount());
        assertEquals(1, first.createdCount());
        assertEquals(1, first.skippedCount());
        assertEquals(1, first.conflictCount());
        assertEquals(1, first.reviewRequiredCount());
        assertEquals(0, first.updatedCount());
        assertEquals(0, first.unchangedCount());
        assertFirstRunState();

        RentalHistoricalBackfillRunResult second = service.createAndRun(
                new RentalHistoricalBackfillCommand(
                        100, 103, 10, 1, false,
                        RentalHistoricalOrderBackfillService.WRITE_CONFIRMATION));

        assertEquals("SUCCEEDED", second.status());
        assertEquals(3, second.scannedCount());
        assertEquals(0, second.createdCount());
        assertEquals(1, second.skippedCount());
        assertEquals(1, second.unchangedCount());
        assertEquals(1, second.conflictCount());
        assertEquals(1, second.reviewRequiredCount());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rental_order WHERE tenant_id = ? AND deleted = b'0'",
                Integer.class, TENANT_ID));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rental_order_item WHERE tenant_id = ? AND deleted = b'0'",
                Integer.class, TENANT_ID));
    }

    private void assertFirstRunState() {
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM rental_order
                 WHERE tenant_id = ?
                   AND source_type = 'XIANYU'
                   AND source_order_id = '20:NORMAL-ORDER'
                   AND deleted = b'0'
                """, Integer.class, TENANT_ID));
        assertEquals("CONFIG_SKIPPED", jdbcTemplate.queryForObject("""
                SELECT conversion_status
                  FROM xianyu_order
                 WHERE id = 102
                """, String.class));
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM rental_order
                 WHERE tenant_id = ?
                   AND source_type = 'XIANYU'
                   AND source_order_id = '20:SKIPPED-ORDER'
                   AND deleted = b'0'
                """, Integer.class, TENANT_ID));
        assertEquals("REVIEW_REQUIRED", jdbcTemplate.queryForObject("""
                SELECT conversion_status
                  FROM xianyu_order
                 WHERE id = 103
                """, String.class));
        assertEquals("FULFILLMENT_ALREADY_RETURNED", jdbcTemplate.queryForObject("""
                SELECT reason_code
                  FROM rental_manual_review
                 WHERE tenant_id = ?
                   AND source_identifier = '103'
                   AND review_type = 'FULFILLMENT_UPDATE'
                   AND deleted = b'0'
                """, String.class, TENANT_ID));
        assertEquals("2026-08-29", jdbcTemplate.queryForObject("""
                SELECT CAST(expected_send_back_date AS CHAR)
                  FROM rental_order
                 WHERE id = 9001
                """, String.class));
        assertEquals("RETURNED", jdbcTemplate.queryForObject("""
                SELECT status
                  FROM rental_device_assignment
                 WHERE id = 9005
                """, String.class));
    }

    private void resetFixture() {
        jdbcTemplate.update("DELETE FROM rental_historical_reconciliation_failure");
        jdbcTemplate.update("DELETE FROM rental_historical_reconciliation_run");
        jdbcTemplate.update("DELETE FROM rental_manual_review");
        jdbcTemplate.update("DELETE FROM rental_device_assignment");
        jdbcTemplate.update("DELETE FROM rental_schedule");
        jdbcTemplate.update("DELETE FROM rental_device_lock");
        jdbcTemplate.update("DELETE FROM rental_order_item");
        jdbcTemplate.update("DELETE FROM rental_order");
        jdbcTemplate.update("DELETE FROM rental_channel_product_rule");
        jdbcTemplate.update("DELETE FROM rental_device_model");
        jdbcTemplate.update("DELETE FROM rental_device");
        jdbcTemplate.update("DELETE FROM xianyu_order");
    }

    private void insertFixture() {
        jdbcTemplate.update("""
                INSERT INTO rental_device_model
                    (id, tenant_id, category_id, model_code, model_name, device_no_prefix,
                     next_sequence, sort_order, enabled, lock_version, creator, updater, deleted)
                VALUES
                    (11, ?, 1, 'A7M4', 'A7M4', 'A7M4', 1, 10, b'1', 0, '', '', b'0')
                """, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_channel_product_rule
                    (id, tenant_id, shop_id, xianyu_item_id, xgj_product_id,
                     product_title_snapshot, handling_policy, mapping_mode,
                     single_device_model_id, enabled, lock_version, creator, updater, deleted)
                VALUES
                    (21, ?, 20, 'ITEM-NORMAL', 'PRODUCT-NORMAL', 'Normal rental',
                     'CREATE_RENTAL', 'SINGLE', 11, b'1', 0, '', '', b'0'),
                    (22, ?, 20, 'ITEM-SKIPPED', 'PRODUCT-SKIPPED', 'Configured skip',
                     'CONFIG_SKIPPED', 'NONE', NULL, b'1', 0, '', '', b'0'),
                    (23, ?, 20, 'ITEM-PROTECTED', 'PRODUCT-PROTECTED', 'Returned rental',
                     'CREATE_RENTAL', 'SINGLE', 11, b'1', 0, '', '', b'0')
                """, TENANT_ID, TENANT_ID, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO xianyu_order
                    (id, tenant_id, shop_id, external_order_id, xgj_product_id, xianyu_item_id,
                     xgj_sku_id, xianyu_sku_id, preparation_status, order_status, pay_amount,
                     currency, seller_remark, remark_parse_version, remark_parse_status,
                     billable_start_date, billable_end_date, ship_date, receive_date, return_date,
                     rental_period_status, conversion_status, rental_order_id, goods_quantity,
                     creator, updater, deleted)
                VALUES
                    (101, ?, 20, 'NORMAL-ORDER', 'PRODUCT-NORMAL', 'ITEM-NORMAL',
                     'SKU-NORMAL', 'XY-SKU-NORMAL', 'WAITING_RECONCILIATION', '12', 12000,
                     'CNY', '#租期9.2-9.4#', 'remark-v2', 'SUCCESS',
                     '2026-09-02', '2026-09-04', '2026-09-01', '2026-09-01', '2026-09-04',
                     'SUCCESS', 'PENDING', NULL, 1, '', '', b'0'),
                    (102, ?, 20, 'SKIPPED-ORDER', 'PRODUCT-SKIPPED', 'ITEM-SKIPPED',
                     'SKU-SKIPPED', 'XY-SKU-SKIPPED', 'WAITING_RECONCILIATION', '12', 8000,
                     'CNY', NULL, NULL, 'PENDING',
                     NULL, NULL, NULL, NULL, NULL,
                     'PENDING', 'PENDING', NULL, 1, '', '', b'0'),
                    (103, ?, 20, 'PROTECTED-ORDER', 'PRODUCT-PROTECTED', 'ITEM-PROTECTED',
                     'SKU-PROTECTED', 'XY-SKU-PROTECTED', 'READY', '12', 15000,
                     'CNY', '#租期9.2-9.6#', 'remark-v2', 'SUCCESS',
                     '2026-09-02', '2026-09-06', '2026-09-01', '2026-09-01', '2026-09-06',
                     'SUCCESS', 'CONVERTED', 9001, 1, '', '', b'0')
                """, TENANT_ID, TENANT_ID, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_order
                    (id, tenant_id, order_no, source_type, source_order_id, channel_order_id,
                     status, rent_amount, refund_amount, settled_at,
                     billable_start_date, billable_end_date, occupy_start_date,
                     occupy_end_date_exclusive, expected_send_back_date,
                     preparation_status, conversion_version, creator, updater, deleted)
                VALUES
                    (9001, ?, 'PROTECTED-9001', 'XIANYU', '20:PROTECTED-ORDER', 103,
                     'PENDING_ALLOCATION', 15000, 0, NULL,
                     '2026-08-27', '2026-08-29', '2026-08-26',
                     '2026-08-30', '2026-08-29',
                     'READY', 'remark-v1', '', '', b'0')
                """, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_order_item
                    (id, tenant_id, rental_order_id, equipment_model_code, source_product_id,
                     source_sku_id, quantity, rent_amount, billable_start_date, billable_end_date,
                     occupy_start_date, occupy_end_date_exclusive, expected_send_back_date,
                     creator, updater, deleted)
                VALUES
                    (9002, ?, 9001, 'A7M4', 'ITEM-PROTECTED',
                     'SKU-PROTECTED', 1, 15000, '2026-08-27', '2026-08-29',
                     '2026-08-26', '2026-08-30', '2026-08-29', '', '', b'0')
                """, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_device
                    (id, tenant_id, device_no, category_code, equipment_model_code,
                     status, enabled, creator, updater, deleted)
                VALUES
                    (9003, ?, 'RETURNED-DEVICE', 'CAMERA', 'A7M4',
                     'AVAILABLE', b'1', '', '', b'0')
                """, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_schedule
                    (id, tenant_id, device_id, rental_order_id, rental_order_item_id,
                     schedule_type, status, occupy_start_date, occupy_end_date_exclusive,
                     idempotency_key, creator, updater, deleted)
                VALUES
                    (9004, ?, 9003, 9001, 9002, 'RENTAL', 'EFFECTIVE',
                     '2026-08-26', '2026-08-30', 'protected-schedule',
                     '', '', b'0')
                """, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_device_assignment
                    (id, tenant_id, rental_order_id, rental_order_item_id, device_id,
                     schedule_id, status, idempotency_key, assigned_at, returned_at,
                     inspection_completed_at, inspection_result, creator, updater, deleted)
                VALUES
                    (9005, ?, 9001, 9002, 9003, 9004, 'RETURNED',
                     'protected-assignment', '2026-08-26 09:00:00',
                     '2026-08-30 09:00:00', '2026-08-30 10:00:00', 'PASSED',
                     '', '', b'0')
                """, TENANT_ID);
    }

    @TestConfiguration
    static class ClockConfiguration {

        @Bean("xianyuClock")
        Clock xianyuClock() {
            return Clock.fixed(
                    Instant.parse("2026-08-31T16:30:00Z"),
                    ZoneId.of("Asia/Shanghai"));
        }
    }
}
