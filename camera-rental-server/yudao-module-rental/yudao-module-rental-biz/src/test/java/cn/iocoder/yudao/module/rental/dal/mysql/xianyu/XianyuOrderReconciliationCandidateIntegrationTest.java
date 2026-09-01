package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = BaseDbUnitTest.Application.class,
        properties = {
                "spring.main.lazy-initialization=true",
                "spring.datasource.url=jdbc:h2:mem:xianyu-reconciliation-candidates;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:/sql/xianyu_order_reconciliation_candidate.sql",
                "mybatis.lazy-initialization=true",
                "yudao.info.base-package=cn.iocoder.yudao.module.rental.dal.mysql"
        })
class XianyuOrderReconciliationCandidateIntegrationTest {

    private static final long TENANT_ID = 9L;

    @Resource
    private XianyuOrderMapper orderMapper;
    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM rental_device_assignment");
        jdbcTemplate.update("DELETE FROM xianyu_order");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void itemCandidatesPagePastFiveHundredWithoutCrossingSafetyBoundaries() {
        for (long id = 1; id <= 502; id++) {
            insertOrder(id, TENANT_ID, 7L, "item-1", "product-1", "sku-1",
                    1_000L + id, "CONVERTED");
        }
        insertOrder(503L, 10L, 7L, "item-1", "product-1", "sku-1",
                1_503L, "CONVERTED");
        insertOrder(504L, TENANT_ID, 8L, "item-1", "product-1", "sku-1",
                1_504L, "CONVERTED");
        insertOrder(505L, TENANT_ID, 7L, "item-1", "product-1", "sku-1",
                1_505L, "CONVERTED");
        jdbcTemplate.update("""
                INSERT INTO rental_device_assignment
                    (id, tenant_id, rental_order_id, deleted)
                VALUES (?, ?, ?, FALSE)
                """, 1L, TENANT_ID, 1_505L);
        insertOrder(506L, TENANT_ID, 7L, "item-1", "product-1", "sku-1",
                1_506L, "CLOSED");
        insertOrder(507L, TENANT_ID, 7L, "item-1", "product-1", "sku-1",
                null, "CONVERTED");

        List<Long> first = orderMapper.selectMutableReconciliationCandidateIdsByItem(
                TENANT_ID, 7L, "item-1", null, 500);
        List<Long> second = orderMapper.selectMutableReconciliationCandidateIdsByItem(
                TENANT_ID, 7L, "item-1", 500L, 500);

        assertEquals(500, first.size());
        assertEquals(1L, first.get(0));
        assertEquals(500L, first.get(499));
        assertEquals(List.of(501L, 502L), second);
    }

    @Test
    void skuCandidatesRequireExactTenantShopProductSkuAndCursor() {
        insertOrder(1L, TENANT_ID, 7L, "item-1", "product-1", "sku-1",
                1_001L, "CONVERTED");
        insertOrder(2L, TENANT_ID, 7L, "item-1", "product-1", "sku-2",
                1_002L, "CONVERTED");
        insertOrder(3L, TENANT_ID, 7L, "item-1", "product-1", "sku-3",
                1_003L, "CONVERTED");
        insertOrder(4L, TENANT_ID, 7L, "item-1", "product-2", "sku-1",
                1_004L, "CONVERTED");
        insertOrder(5L, TENANT_ID, 8L, "item-1", "product-1", "sku-1",
                1_005L, "CONVERTED");

        List<Long> exact = orderMapper.selectMutableReconciliationCandidateIdsByProductAndSkus(
                TENANT_ID, 7L, "product-1", List.of("sku-1", "sku-2"), null, 500);
        List<Long> afterFirst = orderMapper.selectMutableReconciliationCandidateIdsByProductAndSkus(
                TENANT_ID, 7L, "product-1", List.of("sku-1", "sku-2"), 1L, 500);

        assertEquals(List.of(1L, 2L), exact);
        assertEquals(List.of(2L), afterFirst);
    }

    private void insertOrder(Long id, Long tenantId, Long shopId, String itemId,
                             String productId, String skuId, Long rentalOrderId,
                             String conversionStatus) {
        jdbcTemplate.update("""
                INSERT INTO xianyu_order
                    (id, tenant_id, shop_id, xianyu_item_id, xgj_product_id,
                     xgj_sku_id, rental_order_id, conversion_status, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE)
                """, id, tenantId, shopId, itemId, productId, skuId,
                rentalOrderId, conversionStatus);
    }

}
