package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceReturnReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffWarehouseVO.*;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.io.ClassPathResource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="JZD_WORKFLOW_MYSQL_URL", matches="jdbc:mysql://127\\.0\\.0\\.1:33316/jzd_workflow_test.*")
@SpringBootTest(classes=BaseDbUnitTest.Application.class, properties={
        "spring.main.lazy-initialization=true", "spring.sql.init.mode=never", "mybatis.lazy-initialization=true",
        "yudao.info.base-package=cn.iocoder.yudao.module.rental.dal.mysql", "mybatis-plus.global-config.db-config.id-type=AUTO"})
@Import({RentalDeviceOpsService.class,RentalDeviceLockService.class,RentalStaffStocktakeService.class,
        RentalStaffIssueService.class,RentalStaffTaskService.class,RentalShipmentAttemptService.class})
class RentalStaffWorkflowMysqlTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("JZD_WORKFLOW_MYSQL_URL"));
        r.add("spring.datasource.username", () -> "root"); r.add("spring.datasource.password", () -> "");
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
    @Autowired javax.sql.DataSource ds;
    @Autowired RentalDeviceOpsService ops;
    @Autowired RentalStaffStocktakeService stock;
    @Autowired RentalStaffTaskService tasks;
    @Autowired RentalShipmentAttemptService attempts;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MybatisPlusInterceptor interceptor;
    @Autowired cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalStaffOrderMapper staffOrders;
    JdbcTemplate db;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(9L);
        if (interceptor.getInterceptors().stream().noneMatch(TenantLineInnerInterceptor.class::isInstance))
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override public Expression getTenantId() { return new LongValue(TenantContextHolder.getRequiredTenantId()); }
            }));
        db=new JdbcTemplate(ds);
        runSql(new String(new ClassPathResource("sql/staff-workflow-mysql-fixture.sql").getInputStream().readAllBytes(),StandardCharsets.UTF_8));
        Path base=Path.of("").toAbsolutePath();
        while(base!=null && !Files.exists(base.resolve("sql/mysql/migrations/20260922_060_staff_warehouse_workflows.sql"))) base=base.getParent();
        assertNotNull(base);
        for (String table : List.of("rental_staff_stocktake_line", "rental_staff_stocktake", "rental_staff_issue", "rental_staff_inspection", "rental_staff_photo", "rental_staff_inspection_template", "rental_shipment_attempt")) db.execute("DROP TABLE IF EXISTS " + table);
        runSql(Files.readString(base.resolve("sql/mysql/migrations/20260922_060_staff_warehouse_workflows.sql")));
        for(String table:List.of("rental_staff_stocktake_line","rental_staff_stocktake","rental_staff_issue","rental_staff_inspection","rental_staff_photo","rental_shipment_attempt","rental_device_lock","rental_device_assignment","rental_schedule","rental_order_item","rental_order","xianyu_order","rental_device")) db.update("DELETE FROM "+table);
        db.update("INSERT INTO rental_device(id,tenant_id,device_no,equipment_model_code,status,warehouse_code,enabled) VALUES(1,9,'TEST-1','MODEL','RENTED','A',1),(2,9,'TEST-2','MODEL','AVAILABLE','B',1)");
        db.update("INSERT INTO rental_order(id,tenant_id,order_no,source_type,status,occupy_start_date,occupy_end_date_exclusive) VALUES(1,9,'TEST-ORDER','OFFLINE','PENDING_ALLOCATION',CURRENT_DATE(),DATE_ADD(CURRENT_DATE(),INTERVAL 1 DAY))");
        db.update("INSERT INTO rental_order_item(id,tenant_id,rental_order_id,equipment_model_code,quantity) VALUES(1,9,1,'MODEL',1)");
        db.update("INSERT INTO rental_device_assignment(id,tenant_id,rental_order_id,rental_order_item_id,device_id,status) VALUES(1,9,1,1,1,'DISPATCHED')");
    }
    void runSql(String sql) { for(String statement:sql.replaceAll("(?m)^--.*$", "").split(";")) if(!statement.isBlank()) db.execute(statement); }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void channelSearchIsTenantScopedAndExcludesLinkedOrdersWithoutGuessingQuantity() {
        db.update("INSERT INTO xianyu_order(id,tenant_id,external_order_id,goods_title,receiver_mobile,receiver_name,conversion_status,rental_period_reason_code,order_status) VALUES(10,9,'TEST-CHANNEL-10','TEST 100% camera','19900001234','TEST-NAME','REVIEW_REQUIRED','LOGISTICS_DATE_BEFORE_ORDER','12'),(11,9,'TEST-CHANNEL-11','TEST camera','19900001234','TEST-NAME','PENDING',NULL,'12'),(12,10,'OTHER-TENANT','TEST camera','19900001234','TEST-NAME','PENDING',NULL,'12')");
        db.update("UPDATE rental_order SET channel_order_id=11 WHERE id=1");
        assertEquals(1,staffOrders.countChannel(9L,"%1234%",null,null));
        var rows=staffOrders.selectChannelPage(9L,"%1234%",0,20,null,null);
        assertEquals(1,rows.size()); assertEquals(10L,rows.get(0).getChannelOrderId());
        assertNull(rows.get(0).getId()); assertNull(rows.get(0).getRequiredQuantity());
        assertEquals("LOGISTICS_DATE_BEFORE_ORDER",rows.get(0).getPreparationReasonCode());
        assertEquals(1,staffOrders.countChannel(9L,"%100!%%",null,null));
        assertEquals(0,staffOrders.countChannel(9L,"%19900001235%",null,null));
        assertTrue(staffOrders.selectChannelPage(9L,"%1234%",1,20,null,null).isEmpty());
        db.update("UPDATE xianyu_order SET rental_order_id=1 WHERE id=10");
        assertEquals(1,staffOrders.countChannel(9L,null,null,null), "stale channel link must not hide an unconverted record");
        db.update("INSERT INTO rental_order(id,tenant_id,order_no,source_type,status,channel_order_id) VALUES(2,9,'TEST-CONVERTED','XIANYU','PENDING_ALLOCATION',10)");
        assertEquals(0,staffOrders.countChannel(9L,null,null,null));
    }

    @Test void receiveThenInspectUsesRealRowsAndTaskQueues() {
        PageParam page=new PageParam(); page.setPageNo(1);page.setPageSize(20);
        assertEquals(1,tasks.page(page,"RETURN").getTotal());
        var req=new RentalDeviceReturnReqVO();req.setDeviceId(1L);req.setAssignmentId(1L);
        ops.receiveDevice(req);
        assertEquals(0,tasks.page(page,"RETURN").getTotal()); assertEquals(1,tasks.page(page,"INSPECT").getTotal());
        assertEquals("RENTED",db.queryForObject("SELECT status FROM rental_device WHERE id=1",String.class));
        req.setInspectPassed(false); ops.inspectDevice(req);
        assertEquals(0,tasks.page(page,"INSPECT").getTotal()); assertEquals(1,tasks.page(page,"REPAIR").getTotal());
        req.setInspectPassed(true);ops.inspectDevice(req);
        assertEquals(0,tasks.page(page,"REPAIR").getTotal());
        assertEquals("AVAILABLE",db.queryForObject("SELECT status FROM rental_device WHERE id=1",String.class));
    }
    @Test void concurrentReceiveOnlyCreatesOneInspectionLock() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Void> receive=() -> { TenantContextHolder.setTenantId(9L); try { var req=new RentalDeviceReturnReqVO();req.setDeviceId(1L);req.setAssignmentId(1L);ops.receiveDevice(req);return null; } finally {TenantContextHolder.clear();} };
            var a=pool.submit(receive);var b=pool.submit(receive);a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM rental_device_lock WHERE lock_type='RETURN_INSPECTION' AND status='ACTIVE'",Integer.class));
        } finally { pool.shutdownNow(); }
    }
    @Test void attemptSurvivesOuterTransactionRollbackAndBlocksNewKey() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            attempts.begin(10L,"attempt-one","hash");throw new IllegalStateException("simulate local commit failure");
        }));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM rental_shipment_attempt",Integer.class));
        assertThrows(ServiceException.class, () -> attempts.begin(10L,"attempt-two","hash"));
        attempts.rejected(10L,"attempt-one"); attempts.begin(10L,"attempt-two","hash");
        assertEquals("UNKNOWN",attempts.status(10L,"attempt-two"));
    }
    @Test void stocktakePersistsDiffAndTenantIsolation() {
        var req=new StocktakeCreate();req.setWarehouseCode("A");req.setIdempotencyKey("test-stock");
        Long id=stock.create(req);
        assertTrue(stock.get(id).getLines().isEmpty(),"rented device is not physically expected in warehouse");
        var scan=new StocktakeAction();scan.setId(id);scan.setDeviceId(2L);stock.scan(scan);stock.scan(scan);
        assertEquals(1,stock.get(id).getLines().size());assertFalse(stock.get(id).getLines().get(0).getExpected());
        assertEquals("B",db.queryForObject("SELECT warehouse_code FROM rental_device WHERE id=2",String.class));
        stock.close(id);stock.move(scan);assertEquals("A",db.queryForObject("SELECT warehouse_code FROM rental_device WHERE id=2",String.class));
        TenantContextHolder.setTenantId(10L);assertThrows(ServiceException.class, () -> stock.get(id));
    }
}
