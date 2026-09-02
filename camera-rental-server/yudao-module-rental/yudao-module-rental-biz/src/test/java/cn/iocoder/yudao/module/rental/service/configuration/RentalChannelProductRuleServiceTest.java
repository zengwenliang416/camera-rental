package cn.iocoder.yudao.module.rental.service.configuration;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleImpactRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSaveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSkuReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleStatusReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductRuleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductSkuMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelRuleImpactDO;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_PRODUCT_NOT_SYNCHRONIZED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_PRODUCT_RULE_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_RECONCILIATION_ACTIVE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_SKU_OWNERSHIP_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CONFIGURATION_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHIP_PRODUCT_RULE_BIND_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalChannelProductRuleServiceTest {

    private RentalChannelProductRuleMapper ruleMapper;
    private RentalChannelProductSkuMappingMapper mappingMapper;
    private XianyuShopMapper shopMapper;
    private XianyuApplicationMapper applicationMapper;
    private XianyuProductMapper productMapper;
    private XianyuProductSkuMapper productSkuMapper;
    private RentalDeviceModelMapper modelMapper;
    private RentalChannelOrderReconciliationTrigger reconciliationTrigger;
    private RentalChannelReconciliationRunService reconciliationRunService;
    private RentalChannelProductRuleService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        ruleMapper = mock(RentalChannelProductRuleMapper.class);
        mappingMapper = mock(RentalChannelProductSkuMappingMapper.class);
        shopMapper = mock(XianyuShopMapper.class);
        applicationMapper = mock(XianyuApplicationMapper.class);
        productMapper = mock(XianyuProductMapper.class);
        productSkuMapper = mock(XianyuProductSkuMapper.class);
        modelMapper = mock(RentalDeviceModelMapper.class);
        reconciliationTrigger = mock(RentalChannelOrderReconciliationTrigger.class);
        reconciliationRunService = mock(RentalChannelReconciliationRunService.class);
        RentalChannelProductRuleValidator validator = new RentalChannelProductRuleValidator(
                shopMapper, applicationMapper, productMapper, productSkuMapper, modelMapper);
        service = new RentalChannelProductRuleService(
                ruleMapper, mappingMapper, validator, reconciliationTrigger,
                reconciliationRunService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsSingleModelRuleFromExactShopAndItem() {
        stubExactSource();
        when(modelMapper.selectById(300L)).thenReturn(enabledModel(300L));
        when(ruleMapper.insert(any(RentalChannelProductRuleDO.class))).thenAnswer(invocation -> {
            RentalChannelProductRuleDO rule = invocation.getArgument(0);
            rule.setId(400L);
            return 1;
        });
        when(ruleMapper.selectImpact(20L, "1062409679830"))
                .thenReturn(impact(12L, 4L, 5L, 3L, 2L));
        when(reconciliationTrigger.afterRuleChange(400L, 20L, "1062409679830"))
                .thenReturn(900L);
        var result = service.createRule(singleRule());

        assertEquals(400L, result.getRuleId());
        assertEquals(0, result.getLockVersion());
        assertEquals(900L, result.getReconciliationRunId());
        assertEquals(12L, result.getImpact().getScannedCount());
        ArgumentCaptor<RentalChannelProductRuleDO> captor =
                ArgumentCaptor.forClass(RentalChannelProductRuleDO.class);
        verify(ruleMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getTenantId());
        assertEquals("P-100", captor.getValue().getXgjProductId());
        assertEquals("1062409679830", captor.getValue().getXianyuItemId());
        assertEquals("CREATE_RENTAL", captor.getValue().getHandlingPolicy());
        assertEquals("SINGLE", captor.getValue().getMappingMode());
        assertEquals(300L, captor.getValue().getSingleDeviceModelId());
        verify(mappingMapper, never()).insert(any(RentalChannelProductSkuMappingDO.class));
        verify(reconciliationTrigger).afterRuleChange(400L, 20L, "1062409679830");
    }

    @Test
    void shipmentRuleCreationDoesNotStartAsynchronousReconciliation() {
        stubExactSource();
        when(modelMapper.selectById(300L)).thenReturn(enabledModel(300L));
        when(ruleMapper.insert(any(RentalChannelProductRuleDO.class))).thenAnswer(invocation -> {
            RentalChannelProductRuleDO rule = invocation.getArgument(0);
            rule.setId(410L);
            return 1;
        });

        Long ruleId = service.createSingleRuleFromShipment(20L, "1062409679830", 300L);

        assertEquals(410L, ruleId);
        ArgumentCaptor<RentalChannelProductRuleDO> captor =
                ArgumentCaptor.forClass(RentalChannelProductRuleDO.class);
        verify(ruleMapper).insert(captor.capture());
        assertEquals("CREATE_RENTAL", captor.getValue().getHandlingPolicy());
        assertEquals("SINGLE", captor.getValue().getMappingMode());
        assertEquals(300L, captor.getValue().getSingleDeviceModelId());
        assertEquals("发货时经人工确认，绑定到扫描设备型号", captor.getValue().getRuleNote());
        verify(reconciliationTrigger, never()).afterRuleChange(any(), any(), any());
    }

    @Test
    void shipmentRuleCreationNeverOverwritesExistingRule() {
        when(ruleMapper.selectByShopIdAndItemId(20L, "1062409679830"))
                .thenReturn(existingRule(20L, "1062409679830"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createSingleRuleFromShipment(20L, "1062409679830", 300L));

        assertEquals(XIANYU_SHIP_PRODUCT_RULE_BIND_CONFLICT.getCode(), ex.getCode());
        verify(productMapper, never()).selectByShopIdAndXianyuItemId(any(), any());
        verify(ruleMapper, never()).insert(any(RentalChannelProductRuleDO.class));
        verify(reconciliationTrigger, never()).afterRuleChange(any(), any(), any());
    }

    @Test
    void createsMultiRuleOnlyFromSynchronizedSkuOwnership() {
        stubExactSource();
        XianyuProductSkuDO sku = sku(200L, "S-200", "XY-200");
        when(productSkuMapper.selectListByProductId(100L)).thenReturn(List.of(sku));
        when(modelMapper.selectById(301L)).thenReturn(enabledModel(301L));
        when(ruleMapper.insert(any(RentalChannelProductRuleDO.class))).thenAnswer(invocation -> {
            RentalChannelProductRuleDO rule = invocation.getArgument(0);
            rule.setId(401L);
            return 1;
        });
        when(ruleMapper.selectImpact(20L, "1062409679830"))
                .thenReturn(impact(1L, 1L, 0L, 0L, 0L));

        var result = service.createRule(multiRule(200L));

        assertEquals(401L, result.getRuleId());
        ArgumentCaptor<RentalChannelProductSkuMappingDO> mappingCaptor =
                ArgumentCaptor.forClass(RentalChannelProductSkuMappingDO.class);
        verify(mappingMapper).insert(mappingCaptor.capture());
        assertEquals(401L, mappingCaptor.getValue().getProductRuleId());
        assertEquals(200L, mappingCaptor.getValue().getProductSkuId());
        assertEquals("S-200", mappingCaptor.getValue().getXgjSkuId());
        assertEquals("XY-200", mappingCaptor.getValue().getXianyuSkuId());
        assertEquals(301L, mappingCaptor.getValue().getDeviceModelId());
    }

    @Test
    void rejectsSkuFromAnotherProductBeforeRuleMutation() {
        stubExactSource();
        when(productSkuMapper.selectListByProductId(100L))
                .thenReturn(List.of(sku(201L, "OTHER", "XY-OTHER")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createRule(multiRule(200L)));

        assertEquals(RENTAL_CHANNEL_SKU_OWNERSHIP_INVALID.getCode(), ex.getCode());
        verify(ruleMapper, never()).insert(any(RentalChannelProductRuleDO.class));
        verify(mappingMapper, never()).insert(any(RentalChannelProductSkuMappingDO.class));
    }

    @Test
    void rejectsMissingOrCrossTenantShopBeforeProductLookup() {
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createRule(singleRule()));

        assertEquals(XIANYU_SHOP_NOT_EXISTS.getCode(), ex.getCode());
        verify(productMapper, never()).selectByShopIdAndXianyuItemId(any(), any());
    }

    @Test
    void rejectsShopWhoseApplicationIsMissingOrCrossTenant() {
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(applicationMapper.selectByTenantIdAndId(9L, 10L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createRule(singleRule()));

        assertEquals(XIANYU_SHOP_AUTHORIZATION_INVALID.getCode(), ex.getCode());
        verify(productMapper, never()).selectByShopIdAndXianyuItemId(any(), any());
    }

    @Test
    void rejectsExpiredShopAuthorizationAtRuntime() {
        XianyuShopDO shop = validShop();
        shop.setAuthorizationExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(shop);
        when(applicationMapper.selectByTenantIdAndId(9L, 10L)).thenReturn(application());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createRule(singleRule()));

        assertEquals(XIANYU_SHOP_AUTHORIZATION_INVALID.getCode(), ex.getCode());
        verify(productMapper, never()).selectByShopIdAndXianyuItemId(any(), any());
    }

    @Test
    void rejectsUnsynchronizedItemWithoutIdentifierFallback() {
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(applicationMapper.selectByTenantIdAndId(9L, 10L)).thenReturn(application());
        when(productMapper.selectByShopIdAndXianyuItemId(20L, "1062409679830"))
                .thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createRule(singleRule()));

        assertEquals(RENTAL_CHANNEL_PRODUCT_NOT_SYNCHRONIZED.getCode(), ex.getCode());
        verify(productMapper, never()).selectByShopIdAndXgjProductId(any(), any());
    }

    @Test
    void skippedRuleClearsModelConfigurationAndStillReturnsImpact() {
        stubExactSource();
        when(ruleMapper.insert(any(RentalChannelProductRuleDO.class))).thenAnswer(invocation -> {
            RentalChannelProductRuleDO rule = invocation.getArgument(0);
            rule.setId(402L);
            return 1;
        });
        when(ruleMapper.selectImpact(20L, "1062409679830"))
                .thenReturn(impact(8L, 6L, 1L, 1L, 1L));
        RentalChannelProductRuleSaveReqVO reqVO = singleRule();
        reqVO.setHandlingPolicy("CONFIG_SKIPPED");
        reqVO.setMappingMode(null);
        reqVO.setSingleDeviceModelId(300L);
        RentalChannelProductRuleSkuReqVO ignoredSku = new RentalChannelProductRuleSkuReqVO();
        ignoredSku.setProductSkuId(999L);
        ignoredSku.setDeviceModelId(998L);
        ignoredSku.setEnabled(true);
        reqVO.setSkuMappings(List.of(ignoredSku));

        var result = service.createRule(reqVO);

        assertEquals(6L, result.getImpact().getWithoutInternalOrderCount());
        ArgumentCaptor<RentalChannelProductRuleDO> captor =
                ArgumentCaptor.forClass(RentalChannelProductRuleDO.class);
        verify(ruleMapper).insert(captor.capture());
        assertEquals("NONE", captor.getValue().getMappingMode());
        assertEquals(null, captor.getValue().getSingleDeviceModelId());
        verify(modelMapper, never()).selectById(any());
        verify(productSkuMapper, never()).selectListByProductId(any());
    }

    @Test
    void staleRuleVersionCannotOverwriteNewerConfiguration() {
        stubExactSource();
        when(modelMapper.selectById(300L)).thenReturn(enabledModel(300L));
        when(ruleMapper.selectById(400L))
                .thenReturn(existingRule(20L, "1062409679830"));
        when(ruleMapper.updateByIdAndVersion(any(RentalChannelProductRuleDO.class), eq(9L), eq(2)))
                .thenReturn(0);
        RentalChannelProductRuleSaveReqVO reqVO = singleRule();
        reqVO.setId(400L);
        reqVO.setLockVersion(2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateRule(reqVO));

        assertEquals(RENTAL_CONFIGURATION_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(ruleMapper, never()).updateById(any(RentalChannelProductRuleDO.class));
        verify(mappingMapper, never()).deleteByProductRuleId(any());
        verify(mappingMapper, never()).insert(any(RentalChannelProductSkuMappingDO.class));
    }

    @Test
    void activeReconciliationBlocksRuleUpdateBeforeValidationOrMutation() {
        when(ruleMapper.selectById(400L))
                .thenReturn(existingRule(20L, "1062409679830"));
        org.mockito.Mockito.doThrow(new ServiceException(
                        RENTAL_CHANNEL_RECONCILIATION_ACTIVE.getCode(),
                        RENTAL_CHANNEL_RECONCILIATION_ACTIVE.getMsg()))
                .when(reconciliationRunService).assertNoActiveRuleRun(400L);
        RentalChannelProductRuleSaveReqVO reqVO = singleRule();
        reqVO.setId(400L);
        reqVO.setLockVersion(2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateRule(reqVO));

        assertEquals(RENTAL_CHANNEL_RECONCILIATION_ACTIVE.getCode(), ex.getCode());
        verify(productMapper, never()).selectByShopIdAndXianyuItemId(any(), any());
        verify(ruleMapper, never()).updateByIdAndVersion(any(), any(), any());
        verify(mappingMapper, never()).deleteByProductRuleId(any());
    }

    @Test
    void successfulRuleUpdateReplacesSkuMappingsOnlyAfterVersionMatch() {
        stubExactSource();
        when(ruleMapper.selectById(400L))
                .thenReturn(existingRule(20L, "1062409679830"));
        XianyuProductSkuDO sku = sku(200L, "S-200", "XY-200");
        when(productSkuMapper.selectListByProductId(100L)).thenReturn(List.of(sku));
        when(modelMapper.selectById(301L)).thenReturn(enabledModel(301L));
        when(ruleMapper.updateByIdAndVersion(any(RentalChannelProductRuleDO.class), eq(9L), eq(2)))
                .thenReturn(1);
        when(ruleMapper.selectImpact(20L, "1062409679830"))
                .thenReturn(impact(1L, 0L, 1L, 0L, 0L));
        RentalChannelProductRuleSaveReqVO reqVO = multiRule(200L);
        reqVO.setId(400L);
        reqVO.setLockVersion(2);

        var result = service.updateRule(reqVO);

        assertEquals(3, result.getLockVersion());
        verify(mappingMapper).deleteByProductRuleId(400L);
        verify(mappingMapper).insert(any(RentalChannelProductSkuMappingDO.class));
        verify(reconciliationTrigger).afterRuleChange(400L, 20L, "1062409679830");
    }

    @Test
    void changingRuleIdentityReconcilesBothOldAndNewExactScopes() {
        stubExactSource();
        when(ruleMapper.selectById(400L))
                .thenReturn(existingRule(21L, "old-item"));
        when(modelMapper.selectById(300L)).thenReturn(enabledModel(300L));
        when(ruleMapper.updateByIdAndVersion(any(RentalChannelProductRuleDO.class), eq(9L), eq(2)))
                .thenReturn(1);
        when(ruleMapper.selectImpact(20L, "1062409679830"))
                .thenReturn(impact(2L, 0L, 2L, 0L, 0L));
        RentalChannelProductRuleSaveReqVO reqVO = singleRule();
        reqVO.setId(400L);
        reqVO.setLockVersion(2);

        service.updateRule(reqVO);

        verify(reconciliationTrigger).afterRuleChange(400L, 21L, "old-item");
        verify(reconciliationTrigger).afterRuleChange(400L, 20L, "1062409679830");
    }

    @Test
    void updateRuleMapsShopItemCollisionToStableDomainErrorBeforeChildReplacement() {
        stubExactSource();
        when(modelMapper.selectById(300L)).thenReturn(enabledModel(300L));
        when(ruleMapper.selectById(400L))
                .thenReturn(existingRule(20L, "1062409679830"));
        when(ruleMapper.updateByIdAndVersion(any(RentalChannelProductRuleDO.class), eq(9L), eq(2)))
                .thenThrow(new DuplicateKeyException("uk_tenant_shop_item"));
        RentalChannelProductRuleSaveReqVO reqVO = singleRule();
        reqVO.setId(400L);
        reqVO.setLockVersion(2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateRule(reqVO));

        assertEquals(RENTAL_CHANNEL_PRODUCT_RULE_DUPLICATE.getCode(), ex.getCode());
        verify(mappingMapper, never()).deleteByProductRuleId(any());
        verify(mappingMapper, never()).insert(any(RentalChannelProductSkuMappingDO.class));
    }

    @Test
    void impactPreviewIsBoundedAndContainsNoOrderDetails() {
        stubExactSource();
        when(ruleMapper.selectImpact(20L, "1062409679830"))
                .thenReturn(impact(120L, 30L, 50L, 40L, 7L));

        RentalChannelProductRuleImpactRespVO impact =
                service.previewImpact(20L, "1062409679830");

        assertEquals(120L, impact.getScannedCount());
        assertEquals(30L, impact.getWithoutInternalOrderCount());
        assertEquals(50L, impact.getMutableInternalOrderCount());
        assertEquals(40L, impact.getProtectedOrderCount());
        assertEquals(7L, impact.getReviewRequiredCount());
    }

    @Test
    void enablingRuleAutomaticallyReconcilesExactWaitingOrders() {
        RentalChannelProductRuleDO current = RentalChannelProductRuleDO.builder()
                .id(400L)
                .shopId(20L)
                .xianyuItemId("1062409679830")
                .handlingPolicy("CREATE_RENTAL")
                .mappingMode("SINGLE")
                .singleDeviceModelId(300L)
                .enabled(false)
                .lockVersion(2)
                .build();
        when(ruleMapper.selectById(400L)).thenReturn(current);
        stubExactSource();
        when(modelMapper.selectById(300L)).thenReturn(enabledModel(300L));
        when(ruleMapper.updateByIdAndVersion(any(RentalChannelProductRuleDO.class), eq(9L), eq(2)))
                .thenReturn(1);
        RentalChannelProductRuleStatusReqVO request = new RentalChannelProductRuleStatusReqVO();
        request.setId(400L);
        request.setEnabled(true);
        request.setLockVersion(2);

        when(ruleMapper.selectImpact(20L, "1062409679830"))
                .thenReturn(impact(1L, 0L, 1L, 0L, 0L));
        when(reconciliationTrigger.afterRuleChange(400L, 20L, "1062409679830"))
                .thenReturn(901L);

        var result = service.updateRuleStatus(request);

        assertEquals(3, result.getLockVersion());
        assertEquals(901L, result.getReconciliationRunId());
        verify(reconciliationTrigger).afterRuleChange(400L, 20L, "1062409679830");
    }

    private void stubExactSource() {
        when(shopMapper.selectByTenantIdAndId(9L, 20L)).thenReturn(validShop());
        when(applicationMapper.selectByTenantIdAndId(9L, 10L)).thenReturn(application());
        when(productMapper.selectByShopIdAndXianyuItemId(20L, "1062409679830"))
                .thenReturn(product());
    }

    private static RentalChannelProductRuleSaveReqVO singleRule() {
        RentalChannelProductRuleSaveReqVO reqVO = new RentalChannelProductRuleSaveReqVO();
        reqVO.setShopId(20L);
        reqVO.setXianyuItemId("1062409679830");
        reqVO.setHandlingPolicy("CREATE_RENTAL");
        reqVO.setMappingMode("SINGLE");
        reqVO.setSingleDeviceModelId(300L);
        reqVO.setEnabled(true);
        reqVO.setRuleNote("测试");
        return reqVO;
    }

    private static RentalChannelProductRuleSaveReqVO multiRule(Long productSkuId) {
        RentalChannelProductRuleSaveReqVO reqVO = singleRule();
        reqVO.setMappingMode("MULTI");
        reqVO.setSingleDeviceModelId(null);
        RentalChannelProductRuleSkuReqVO skuReqVO = new RentalChannelProductRuleSkuReqVO();
        skuReqVO.setProductSkuId(productSkuId);
        skuReqVO.setDeviceModelId(301L);
        skuReqVO.setEnabled(true);
        reqVO.setSkuMappings(List.of(skuReqVO));
        return reqVO;
    }

    private static XianyuShopDO validShop() {
        return XianyuShopDO.builder()
                .id(20L)
                .applicationId(10L)
                .shopName("小疆")
                .authorizationStatus("VALID")
                .build();
    }

    private static XianyuApplicationDO application() {
        XianyuApplicationDO application = XianyuApplicationDO.builder()
                .id(10L)
                .applicationCode("default")
                .build();
        application.setTenantId(9L);
        return application;
    }

    private static XianyuProductDO product() {
        return XianyuProductDO.builder()
                .id(100L)
                .shopId(20L)
                .xgjProductId("P-100")
                .xianyuItemId("1062409679830")
                .title("测试商品")
                .build();
    }

    private static XianyuProductSkuDO sku(Long id, String xgjSkuId, String xianyuSkuId) {
        return XianyuProductSkuDO.builder()
                .id(id)
                .productId(100L)
                .xgjSkuId(xgjSkuId)
                .xianyuSkuId(xianyuSkuId)
                .skuName("规格")
                .build();
    }

    private static RentalChannelProductRuleDO existingRule(Long shopId, String xianyuItemId) {
        return RentalChannelProductRuleDO.builder()
                .id(400L)
                .shopId(shopId)
                .xianyuItemId(xianyuItemId)
                .handlingPolicy("CREATE_RENTAL")
                .mappingMode("SINGLE")
                .singleDeviceModelId(300L)
                .enabled(true)
                .lockVersion(2)
                .build();
    }

    private static RentalDeviceModelDO enabledModel(Long id) {
        return RentalDeviceModelDO.builder()
                .id(id)
                .categoryId(1L)
                .modelCode("MODEL-" + id)
                .modelName("型号 " + id)
                .deviceNoPrefix("M" + id)
                .enabled(true)
                .build();
    }

    private static RentalChannelRuleImpactDO impact(long scanned, long withoutInternal,
                                                     long mutable, long protectedCount,
                                                     long reviewRequired) {
        return new RentalChannelRuleImpactDO()
                .setScannedCount(scanned)
                .setWithoutInternalOrderCount(withoutInternal)
                .setMutableInternalOrderCount(mutable)
                .setProtectedOrderCount(protectedCount)
                .setReviewRequiredCount(reviewRequired);
    }
}
