package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.*;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import cn.iocoder.yudao.module.rental.service.device.*;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.aop.support.AopUtils;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BaseDbUnitTest.Application.class,
 properties = {"spring.main.lazy-initialization=true",
 "spring.datasource.url=jdbc:h2:mem:device-import;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
 "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:/sql/rental_device_import.sql",
 "mybatis.lazy-initialization=true", "yudao.info.base-package=cn.iocoder.yudao.module.rental.dal.mysql"})
@Import({RentalDeviceImportService.class, RentalDeviceCatalogService.class, RentalDeviceAdminService.class,
 RentalDeviceImportTransactionTest.Config.class})
class RentalDeviceImportTransactionTest {
 @Configuration static class Config {
  @Bean JdbcTemplate jdbc(javax.sql.DataSource source) { return new JdbcTemplate(source); }
  @Bean TenantLineInnerInterceptor tenantLine(com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor interceptor) {
   var tenant = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
   cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils.addInterceptor(interceptor, tenant, 0);
   return tenant;
  }
 }
 @Resource RentalDeviceImportService service;
 @Resource RentalDeviceMapper devices;
 @Resource JdbcTemplate jdbc;
 @Resource TenantLineInnerInterceptor tenantLine;
 @MockitoBean RentalDeviceAssignmentMapper assignments;
 @MockitoBean RentalDeviceDeletionGuard deletionGuard;
 @MockitoBean RentalDeviceAssignmentService assignmentService;
 @MockitoBean RentalDeviceQrCodec qrCodec;
 @MockitoBean RentalDeviceProperties properties;
 @BeforeEach void setup() {
  TenantContextHolder.setTenantId(9L);
  jdbc.update("DELETE FROM rental_device_import_batch"); jdbc.update("DELETE FROM rental_device");
  jdbc.update("DELETE FROM rental_device_model"); jdbc.update("DELETE FROM rental_device_category");
  jdbc.update("INSERT INTO rental_device_category(id,category_code,category_name,enabled,tenant_id) VALUES(1,'DJI','DJI',true,9)");
  jdbc.update("INSERT INTO rental_device_model(id,category_id,model_code,model_name,device_no_prefix,next_sequence,enabled,tenant_id) VALUES(2,1,'A5','A5','A5',1,true,9)");
 }
 @AfterEach void clean() { TenantContextHolder.clear(); }
 RentalDeviceImportReqVO request(String... serials) {
  var req = new RentalDeviceImportReqVO(); req.setMode("CREATE"); var rows = new ArrayList<RentalDeviceImportReqVO.Row>();
  for (String serial : serials) { var row = new RentalDeviceImportReqVO.Row(); row.setFileName("sample.txt");
   row.setLineNumber(rows.size()+1); row.setDeviceNo(""); row.setSerialNumber(serial); row.setCategoryCode("DJI"); row.setEquipmentModelCode("A5"); rows.add(row); }
  req.setRows(rows); return req;
 }
 @Test void commitsOnceAndRejectsForeignOwnerAndTenant() {
  assertTrue(AopUtils.isAopProxy(service));
  var preview = service.preview(request("DEMO0001"),8L);
  assertThrows(RuntimeException.class, () -> service.commit(preview.getBatchId(),99L));
  TenantContextHolder.setTenantId(10L);
  assertThrows(RuntimeException.class, () -> service.commit(preview.getBatchId(),8L));
  TenantContextHolder.setTenantId(9L);
  var ids = service.commit(preview.getBatchId(),8L);
  assertEquals(ids, service.commit(preview.getBatchId(),8L));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM rental_device",Integer.class));
  assertEquals("A5-01",devices.selectById(ids.get(0)).getDeviceNo());
 }
 @Test void failedSecondInsertRollsBackDevicesNumberCursorAndReceipt() {
  var preview = service.preview(request("DEMO0001","FAIL9999"),8L);
  assertThrows(RuntimeException.class, () -> service.commit(preview.getBatchId(),8L));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM rental_device",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT next_sequence FROM rental_device_model WHERE id=2",Integer.class));
  assertNull(jdbc.queryForObject("SELECT result_json FROM rental_device_import_batch WHERE id=?",String.class,preview.getBatchId()));
 }
 @Test void concurrentRetriesUseOneReceipt() throws Exception {
  var preview = service.preview(request("DEMO0001"),8L);
  ExecutorService pool = Executors.newFixedThreadPool(2); var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
  Callable<List<Long>> job = () -> { TenantContextHolder.setTenantId(9L); ready.countDown(); start.await();
   try { return service.commit(preview.getBatchId(),8L); } finally { TenantContextHolder.clear(); } };
  try { var a = pool.submit(job); var b = pool.submit(job); assertTrue(ready.await(5,TimeUnit.SECONDS)); start.countDown();
   assertEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM rental_device",Integer.class));
  } finally { start.countDown(); pool.shutdownNow(); }
 }
 @Test void autoNumberSkipsExistingAndDeletedNumbersWithoutMatchingEmptySerials() {
  jdbc.update("INSERT INTO rental_device(device_no,serial_number,equipment_model_code,enabled,deleted,tenant_id) VALUES('A5-01','','A5',true,false,9),('A5-02',NULL,'A5',true,true,9)");
  var preview = service.preview(request("DEMO0001"),8L);
  var ids = service.commit(preview.getBatchId(),8L);
  assertEquals("A5-03", devices.selectById(ids.get(0)).getDeviceNo());
 }
}
