package cn.iocoder.yudao.module.rental.service.configuration;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSaveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSkuReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductRuleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductSkuMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelProductRuleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelProductSkuMappingMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuApplicationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductSkuMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationTrigger;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelReconciliationRunService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = BaseDbUnitTest.Application.class,
        properties = {
                "spring.main.lazy-initialization=true",
                "spring.datasource.url=jdbc:h2:mem:rental-rule-rollback;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:/sql/rental_channel_product_rule_transaction.sql",
                "mybatis.lazy-initialization=true",
                "yudao.info.base-package=cn.iocoder.yudao.module.rental.dal.mysql"
        })
@Import({RentalChannelProductRuleService.class, RentalChannelProductRuleValidator.class})
class RentalChannelProductRuleTransactionIntegrationTest {

    private static final long TENANT_ID = 9L;
    private static final long APPLICATION_ID = 10L;
    private static final long SHOP_ID = 20L;
    private static final long PRODUCT_ID = 100L;
    private static final long REJECTED_PRODUCT_SKU_ID = 201L;
    private static final long MODEL_ID = 301L;
    private static final long RULE_ID = 400L;

    @Resource
    private RentalChannelProductRuleService service;
    @Resource
    private RentalChannelProductRuleMapper ruleMapper;
    @Resource
    private RentalChannelProductSkuMappingMapper mappingMapper;
    @Resource
    private XianyuApplicationMapper applicationMapper;
    @Resource
    private XianyuShopMapper shopMapper;
    @Resource
    private XianyuProductMapper productMapper;
    @Resource
    private XianyuProductSkuMapper productSkuMapper;
    @Resource
    private RentalDeviceModelMapper modelMapper;
    @MockitoBean
    private RentalChannelOrderReconciliationTrigger reconciliationTrigger;
    @MockitoBean
    private RentalChannelReconciliationRunService reconciliationRunService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        insertValidationFixtures();
        insertExistingRule();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRollbackParentUpdateAndChildReplacementWhenNewChildInsertViolatesConstraint() {
        assertTrue(AopUtils.isAopProxy(service), "service must be invoked through the Spring transaction proxy");

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.updateRule(updateRequest()));

        assertTrue(exception.getMostSpecificCause().getMessage()
                        .toLowerCase()
                        .contains("ck_reject_new_mapping"),
                "failure must come from the new child insert constraint");

        RentalChannelProductRuleDO rule = ruleMapper.selectById(RULE_ID);
        assertEquals(2, rule.getLockVersion());
        assertEquals("Original product", rule.getProductTitleSnapshot());
        assertEquals("before update", rule.getRuleNote());

        List<RentalChannelProductSkuMappingDO> mappings =
                mappingMapper.selectListByProductRuleId(RULE_ID);
        assertEquals(2, mappings.size());
        assertEquals(List.of(501L, 502L),
                mappings.stream().map(RentalChannelProductSkuMappingDO::getId).toList());
        assertEquals(List.of("OLD-SKU-A", "OLD-SKU-B"),
                mappings.stream().map(RentalChannelProductSkuMappingDO::getXgjSkuId).toList());
        assertEquals(List.of(191L, 192L),
                mappings.stream().map(RentalChannelProductSkuMappingDO::getProductSkuId).toList());
    }

    private void insertValidationFixtures() {
        XianyuApplicationDO application = XianyuApplicationDO.builder()
                .id(APPLICATION_ID)
                .applicationCode("test-app")
                .displayName("Test application")
                .enabled(true)
                .authorizationStatus("VALID")
                .build();
        application.setTenantId(TENANT_ID);
        applicationMapper.insert(application);

        XianyuShopDO shop = XianyuShopDO.builder()
                .id(SHOP_ID)
                .applicationId(APPLICATION_ID)
                .externalShopId("shop-20")
                .authorizeId("authorize-20")
                .shopName("Test shop")
                .authorizationStatus("VALID")
                .authorizationExpiresAt(LocalDateTime.now().plusDays(1))
                .build();
        shop.setTenantId(TENANT_ID);
        shopMapper.insert(shop);

        XianyuProductDO product = XianyuProductDO.builder()
                .id(PRODUCT_ID)
                .shopId(SHOP_ID)
                .xgjProductId("PRODUCT-100")
                .xianyuItemId("ITEM-100")
                .title("Updated product")
                .status("ON_SALE")
                .build();
        product.setTenantId(TENANT_ID);
        productMapper.insert(product);

        XianyuProductSkuDO sku = XianyuProductSkuDO.builder()
                .id(REJECTED_PRODUCT_SKU_ID)
                .productId(PRODUCT_ID)
                .xgjSkuId("NEW-SKU")
                .xianyuSkuId("XY-NEW-SKU")
                .skuName("New SKU")
                .status("ON_SALE")
                .build();
        sku.setTenantId(TENANT_ID);
        productSkuMapper.insert(sku);

        RentalDeviceModelDO model = RentalDeviceModelDO.builder()
                .id(MODEL_ID)
                .categoryId(30L)
                .modelCode("MODEL-301")
                .modelName("Model 301")
                .deviceNoPrefix("M301")
                .nextSequence(1)
                .sortOrder(1)
                .enabled(true)
                .lockVersion(0)
                .build();
        model.setTenantId(TENANT_ID);
        modelMapper.insert(model);
    }

    private void insertExistingRule() {
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(RULE_ID)
                .shopId(SHOP_ID)
                .xianyuItemId("ITEM-100")
                .xgjProductId("PRODUCT-100")
                .productTitleSnapshot("Original product")
                .handlingPolicy("CREATE_RENTAL")
                .mappingMode("MULTI")
                .enabled(true)
                .ruleNote("before update")
                .lockVersion(2)
                .build();
        rule.setTenantId(TENANT_ID);
        ruleMapper.insert(rule);

        mappingMapper.insert(existingMapping(501L, 191L, "OLD-SKU-A"));
        mappingMapper.insert(existingMapping(502L, 192L, "OLD-SKU-B"));
    }

    private RentalChannelProductSkuMappingDO existingMapping(
            Long id, Long productSkuId, String xgjSkuId) {
        RentalChannelProductSkuMappingDO mapping =
                RentalChannelProductSkuMappingDO.builder()
                        .id(id)
                        .productRuleId(RULE_ID)
                        .productSkuId(productSkuId)
                        .xgjSkuId(xgjSkuId)
                        .xianyuSkuId("XY-" + xgjSkuId)
                        .deviceModelId(MODEL_ID)
                        .enabled(true)
                        .lockVersion(0)
                        .build();
        mapping.setTenantId(TENANT_ID);
        return mapping;
    }

    private RentalChannelProductRuleSaveReqVO updateRequest() {
        RentalChannelProductRuleSkuReqVO sku = new RentalChannelProductRuleSkuReqVO();
        sku.setProductSkuId(REJECTED_PRODUCT_SKU_ID);
        sku.setDeviceModelId(MODEL_ID);
        sku.setEnabled(true);

        RentalChannelProductRuleSaveReqVO request =
                new RentalChannelProductRuleSaveReqVO();
        request.setId(RULE_ID);
        request.setShopId(SHOP_ID);
        request.setXianyuItemId("ITEM-100");
        request.setHandlingPolicy("CREATE_RENTAL");
        request.setMappingMode("MULTI");
        request.setEnabled(true);
        request.setRuleNote("after update");
        request.setLockVersion(2);
        request.setSkuMappings(List.of(sku));
        return request;
    }

}
