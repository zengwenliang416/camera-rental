package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentServiceImpl;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "RENTAL_FULFILLMENT_MYSQL_JDBC_URL", matches = ".+")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = BaseDbUnitTest.Application.class,
        properties = {
                "spring.main.lazy-initialization=true",
                "spring.sql.init.mode=never",
                "mybatis.lazy-initialization=true",
                "yudao.info.base-package=cn.iocoder.yudao.module.rental.dal.mysql"
        })
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import({
        RentalDeviceAssignmentServiceImpl.class,
        RentalChannelOrderReconciliationService.class,
        RentalDeviceLockService.class,
        RentalFulfillmentUpdateGuard.class,
        RentalOrderPreparationPolicy.class,
        RentalChannelOrderEligibilityPolicy.class,
        RentalRemarkPlanChangeClassifier.class,
        RentalFulfillmentLockOrderMysqlConcurrencyTest.AssignmentPauseAspect.class
})
class RentalFulfillmentLockOrderMysqlConcurrencyTest {

    private static final String JDBC_URL_ENV = "RENTAL_FULFILLMENT_MYSQL_JDBC_URL";
    private static final String USER_ENV = "RENTAL_FULFILLMENT_MYSQL_USER";
    private static final String PASSWORD_ENV = "RENTAL_FULFILLMENT_MYSQL_PASSWORD";
    private static final long TENANT_ID = 9L;

    private static final LocalDate PREVIOUS_BILLABLE_START = LocalDate.of(2026, 9, 6);
    private static final LocalDate PREVIOUS_BILLABLE_END = LocalDate.of(2026, 9, 8);
    private static final LocalDate PREVIOUS_OCCUPY_START = LocalDate.of(2026, 9, 5);
    private static final LocalDate PREVIOUS_RETURN_DATE = LocalDate.of(2026, 9, 8);
    private static final LocalDate EXTENDED_BILLABLE_END = LocalDate.of(2026, 9, 10);
    private static final LocalDate EXTENDED_RETURN_DATE = LocalDate.of(2026, 9, 10);

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv(JDBC_URL_ENV));
        registry.add("spring.datasource.username", () -> System.getenv(USER_ENV));
        registry.add("spring.datasource.password", () -> System.getenv(PASSWORD_ENV));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Resource
    private RentalDeviceAssignmentService assignmentService;
    @Resource
    private RentalChannelOrderReconciliationService reconciliationService;
    @Resource
    private RentalDeviceAssignmentMapper assignmentMapper;
    @Resource
    private RentalScheduleMapper scheduleMapper;
    @Resource
    private RentalOrderMapper orderMapper;
    @Resource
    private RentalOrderItemMapper orderItemMapper;
    @Resource
    private XianyuOrderMapper xianyuOrderMapper;
    @Resource
    private AssignmentPauseAspect assignmentPause;
    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        jdbcTemplate = new JdbcTemplate(dataSource);
        resetFixture();
        assignmentPause.arm();
    }

    @AfterEach
    void tearDown() {
        assignmentPause.release();
        TenantContextHolder.clear();
    }

    @Test
    void actualAssignmentAndReconciliationServicesCommitWithoutDeadlock() throws Exception {
        assertTrue(AopUtils.isAopProxy(assignmentService),
                "assignment must run through the Spring transaction proxy");
        assertTrue(AopUtils.isAopProxy(reconciliationService),
                "reconciliation must run through the Spring transaction proxy");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RentalDeviceAssignmentResult> assignment = executor.submit(this::assignThroughProductionService);
            assertTrue(assignmentPause.awaitScheduleInsert(5, TimeUnit.SECONDS),
                    "assignment did not reach the real schedule Mapper insert");

            Future<RentalChannelOrderReconciliationResult> reconciliation =
                    executor.submit(this::reconcileThroughProductionService);
            Thread.sleep(300);
            assertFalse(reconciliation.isDone(),
                    "reconciliation should wait while assignment holds the rental-order lock");

            assignmentPause.release();

            RentalDeviceAssignmentResult assignmentResult = assignment.get(15, TimeUnit.SECONDS);
            RentalChannelOrderReconciliationResult reconciliationResult =
                    reconciliation.get(15, TimeUnit.SECONDS);

            assertEquals("CONVERTED", reconciliationResult.status());
            assertTrue(reconciliationResult.planApplied());
            assertCommittedState(assignmentResult);
        } finally {
            assignmentPause.release();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private RentalDeviceAssignmentResult assignThroughProductionService() {
        establishOperatorContext();
        try {
            return assignmentService.assign(new RentalDeviceAssignmentCommand(
                    1L,
                    1L,
                    PREVIOUS_OCCUPY_START,
                    PREVIOUS_RETURN_DATE.plusDays(1),
                    "mysql-assign-vs-reconcile"));
        } finally {
            clearOperatorContext();
        }
    }

    private RentalChannelOrderReconciliationResult reconcileThroughProductionService() {
        establishOperatorContext();
        try {
            SellerRemarkRentalPeriod previous = SellerRemarkRentalPeriod.success(
                    "EFFECTIVE_INTERNAL_PLAN",
                    PREVIOUS_BILLABLE_START,
                    PREVIOUS_BILLABLE_END,
                    PREVIOUS_OCCUPY_START,
                    PREVIOUS_BILLABLE_START.minusDays(1),
                    PREVIOUS_RETURN_DATE);
            SellerRemarkRentalPeriod candidate = SellerRemarkRentalPeriod.success(
                    "remark-v2",
                    PREVIOUS_BILLABLE_START,
                    EXTENDED_BILLABLE_END,
                    PREVIOUS_OCCUPY_START,
                    PREVIOUS_BILLABLE_START.minusDays(1),
                    EXTENDED_RETURN_DATE);
            return reconciliationService.reconcile(
                    1L,
                    new RentalRemarkPlanUpdate(previous, candidate, RentalRemarkPlanChangeType.EXTENSION));
        } finally {
            clearOperatorContext();
        }
    }

    private static void establishOperatorContext() {
        TenantContextHolder.setTenantId(TENANT_ID);
        LoginUser loginUser = new LoginUser()
                .setId(1L)
                .setUserType(1)
                .setTenantId(TENANT_ID);
        SecurityFrameworkUtils.setLoginUser(loginUser, new MockHttpServletRequest());
    }

    private static void clearOperatorContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    private void assertCommittedState(RentalDeviceAssignmentResult assignmentResult) {
        List<RentalDeviceAssignmentDO> assignments = assignmentMapper.selectListByOrderItem(1L);
        assertEquals(1, assignments.size());
        assertEquals(assignmentResult.assignmentId(), assignments.get(0).getId());
        assertEquals("ASSIGNED", assignments.get(0).getStatus());

        RentalScheduleDO schedule = scheduleMapper.selectById(assignmentResult.scheduleId());
        assertEquals(PREVIOUS_OCCUPY_START, schedule.getOccupyStartDate());
        assertEquals(EXTENDED_RETURN_DATE.plusDays(1), schedule.getOccupyEndDateExclusive());

        RentalOrderDO order = orderMapper.selectById(1L);
        RentalOrderItemDO item = orderItemMapper.selectById(1L);
        XianyuOrderDO source = xianyuOrderMapper.selectById(1L);
        assertEquals(EXTENDED_BILLABLE_END, order.getBillableEndDate());
        assertEquals(EXTENDED_RETURN_DATE, order.getExpectedSendBackDate());
        assertEquals(EXTENDED_BILLABLE_END, item.getBillableEndDate());
        assertEquals(EXTENDED_RETURN_DATE, item.getExpectedSendBackDate());
        assertEquals("CONVERTED", source.getConversionStatus());
        assertEquals("READY", source.getPreparationStatus());
    }

    private void resetFixture() {
        jdbcTemplate.update("DELETE FROM rental_device_assignment");
        jdbcTemplate.update("DELETE FROM rental_schedule");
        jdbcTemplate.update("DELETE FROM rental_device_lock");
        jdbcTemplate.update("DELETE FROM rental_manual_review");
        jdbcTemplate.update("DELETE FROM rental_order_item");
        jdbcTemplate.update("DELETE FROM rental_order");
        jdbcTemplate.update("DELETE FROM rental_channel_product_rule");
        jdbcTemplate.update("DELETE FROM rental_device_model");
        jdbcTemplate.update("DELETE FROM rental_device");
        jdbcTemplate.update("DELETE FROM xianyu_order");

        jdbcTemplate.update("""
                INSERT INTO xianyu_order
                    (id, tenant_id, shop_id, external_order_id, xgj_product_id, xianyu_item_id,
                     xgj_sku_id, xianyu_sku_id, preparation_status, order_status, pay_amount,
                     currency, seller_remark, remark_parse_version, remark_parse_status,
                     billable_start_date, billable_end_date, ship_date, receive_date, return_date,
                     rental_period_status, conversion_status, rental_order_id, goods_quantity,
                     creator, updater, deleted)
                VALUES
                    (1, ?, 20, 'ORDER-1', 'PRODUCT-1', 'ITEM-1', 'SKU-1', 'XY-SKU-1',
                     'READY', '12', 10000, 'CNY', '#租期9.6-9.10#', 'remark-v2', 'SUCCESS',
                     ?, ?, ?, ?, ?, 'SUCCESS', 'CONVERTED', 1, 1, '', '', b'0')
                """,
                TENANT_ID,
                PREVIOUS_BILLABLE_START,
                EXTENDED_BILLABLE_END,
                PREVIOUS_OCCUPY_START,
                PREVIOUS_BILLABLE_START.minusDays(1),
                EXTENDED_RETURN_DATE);
        jdbcTemplate.update("""
                INSERT INTO rental_order
                    (id, tenant_id, order_no, source_type, source_order_id, channel_order_id,
                     status, rent_amount, refund_amount, billable_start_date, billable_end_date,
                     occupy_start_date, occupy_end_date_exclusive, expected_send_back_date,
                     preparation_status, conversion_version, creator, updater, deleted)
                VALUES
                    (1, ?, 'XY-1', 'XIANYU', '20:ORDER-1', 1, 'PENDING_ALLOCATION',
                     10000, 0, ?, ?, ?, ?, ?, 'READY', 'remark-v1', '', '', b'0')
                """,
                TENANT_ID,
                PREVIOUS_BILLABLE_START,
                PREVIOUS_BILLABLE_END,
                PREVIOUS_OCCUPY_START,
                PREVIOUS_RETURN_DATE.plusDays(1),
                PREVIOUS_RETURN_DATE);
        jdbcTemplate.update("""
                INSERT INTO rental_order_item
                    (id, tenant_id, rental_order_id, equipment_model_code, source_product_id,
                     source_sku_id, quantity, rent_amount, billable_start_date, billable_end_date,
                     occupy_start_date, occupy_end_date_exclusive, expected_send_back_date,
                     creator, updater, deleted)
                VALUES
                    (1, ?, 1, 'P4', 'ITEM-1', 'SKU-1', 1, 10000, ?, ?, ?, ?, ?, '', '', b'0')
                """,
                TENANT_ID,
                PREVIOUS_BILLABLE_START,
                PREVIOUS_BILLABLE_END,
                PREVIOUS_OCCUPY_START,
                PREVIOUS_RETURN_DATE.plusDays(1),
                PREVIOUS_RETURN_DATE);
        jdbcTemplate.update("""
                INSERT INTO rental_device
                    (id, tenant_id, device_no, category_code, equipment_model_code, status,
                     enabled, creator, updater, deleted)
                VALUES (1, ?, 'P4-01', 'DJI', 'P4', 'AVAILABLE', b'1', '', '', b'0')
                """, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_device_model
                    (id, tenant_id, category_id, model_code, model_name, device_no_prefix,
                     next_sequence, sort_order, enabled, lock_version, creator, updater, deleted)
                VALUES (1, ?, 1, 'P4', 'P4', 'P4', 2, 10, b'1', 0, '', '', b'0')
                """, TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO rental_channel_product_rule
                    (id, tenant_id, shop_id, xianyu_item_id, xgj_product_id,
                     product_title_snapshot, handling_policy, mapping_mode,
                     single_device_model_id, enabled, lock_version, creator, updater, deleted)
                VALUES
                    (1, ?, 20, 'ITEM-1', 'PRODUCT-1', 'P4 rental',
                     'CREATE_RENTAL', 'SINGLE', 1, b'1', 0, '', '', b'0')
                """, TENANT_ID);
    }

    @Aspect
    static class AssignmentPauseAspect {

        private final AtomicBoolean armed = new AtomicBoolean();
        private volatile CountDownLatch scheduleInsertReached = new CountDownLatch(1);
        private volatile CountDownLatch allowInsert = new CountDownLatch(1);

        void arm() {
            scheduleInsertReached = new CountDownLatch(1);
            allowInsert = new CountDownLatch(1);
            armed.set(true);
        }

        boolean awaitScheduleInsert(long timeout, TimeUnit unit) throws InterruptedException {
            return scheduleInsertReached.await(timeout, unit);
        }

        void release() {
            allowInsert.countDown();
        }

        @Around("bean(rentalScheduleMapper) && execution(* insert(..))")
        Object pauseFirstScheduleInsert(ProceedingJoinPoint joinPoint) throws Throwable {
            if (armed.compareAndSet(true, false)) {
                scheduleInsertReached.countDown();
                if (!allowInsert.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release schedule insert");
                }
            }
            return joinPoint.proceed();
        }
    }

}
