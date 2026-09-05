package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductRuleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductSkuMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelProductRuleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelProductSkuMappingMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductSkuMapper;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalChannelOrderReconciliationServiceTest {

    @Mock
    private XianyuOrderMapper xianyuOrderMapper;
    @Mock
    private XianyuProductMapper productMapper;
    @Mock
    private XianyuProductSkuMapper productSkuMapper;
    @Mock
    private RentalChannelProductRuleMapper ruleMapper;
    @Mock
    private RentalChannelProductSkuMappingMapper skuMappingMapper;
    @Mock
    private RentalDeviceModelMapper modelMapper;
    @Mock
    private RentalOrderMapper rentalOrderMapper;
    @Mock
    private RentalOrderItemMapper rentalOrderItemMapper;
    @Mock
    private RentalDeviceAssignmentMapper assignmentMapper;
    @Mock
    private RentalDeviceMapper deviceMapper;
    @Mock
    private RentalScheduleMapper scheduleMapper;
    @Mock
    private RentalDeviceLockService deviceLockService;
    @Mock
    private RentalManualReviewMapper manualReviewMapper;

    private RentalChannelOrderReconciliationService service;

    @BeforeEach
    void setUp() {
        RentalFulfillmentUpdateGuard fulfillmentUpdateGuard = new RentalFulfillmentUpdateGuard(
                assignmentMapper, deviceMapper, scheduleMapper, deviceLockService);
        service = new RentalChannelOrderReconciliationService(
                xianyuOrderMapper, productMapper, productSkuMapper, ruleMapper, skuMappingMapper,
                modelMapper, rentalOrderMapper, rentalOrderItemMapper, manualReviewMapper,
                new RentalChannelOrderEligibilityPolicy(), new RentalOrderPreparationPolicy(),
                fulfillmentUpdateGuard, new RentalRemarkPlanChangeClassifier());
    }

    @Test
    void createsInternalOrderImmediatelyWhenModelAndRemarkAreMissing() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalPeriodStatus("PENDING");
        source.setRentalPeriodReasonCode("MISSING_REMARK");
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        assignGeneratedIds();

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("CONVERTED", result.status());
        assertEquals(31L, result.rentalOrderId());
        assertEquals("WAITING_MODEL", result.preparationStatus());
        assertEquals("PRODUCT_RULE_NOT_CONFIGURED", result.reasonCode());
        ArgumentCaptor<RentalOrderDO> orderCaptor = ArgumentCaptor.forClass(RentalOrderDO.class);
        verify(rentalOrderMapper).insert(orderCaptor.capture());
        assertEquals(9L, orderCaptor.getValue().getTenantId());
        assertEquals(4_294_967_296L, orderCaptor.getValue().getRentAmount());
        ArgumentCaptor<RentalOrderItemDO> itemCaptor = ArgumentCaptor.forClass(RentalOrderItemDO.class);
        verify(rentalOrderItemMapper).insert(itemCaptor.capture());
        assertEquals(9L, itemCaptor.getValue().getTenantId());
        assertNull(itemCaptor.getValue().getEquipmentModelCode());
        assertEquals("item-1", itemCaptor.getValue().getSourceProductId());
        assertEquals("sku-1", itemCaptor.getValue().getSourceSkuId());
        assertEquals("CONVERTED", source.getConversionStatus());
        assertEquals("WAITING_MODEL", source.getPreparationStatus());
        verify(manualReviewMapper, never()).insert(any(RentalManualReviewDO.class));
    }

    @Test
    void reusesOneInternalOrderAndBecomesReadyAfterSingleModelAndRemarkArrive() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalOrderId(31L);
        successfulPeriod(source);
        RentalOrderDO existingOrder = RentalOrderDO.builder()
                .id(31L)
                .sourceType("XIANYU")
                .sourceOrderId("7:order-1")
                .channelOrderId(10L)
                .status("PENDING_ALLOCATION")
                .preparationStatus("WAITING_MODEL")
                .build();
        RentalOrderItemDO existingItem = RentalOrderItemDO.builder()
                .id(41L).rentalOrderId(31L).quantity(1).rentAmount(source.getPayAmount()).build();
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L).handlingPolicy("CREATE_RENTAL").mappingMode("SINGLE")
                .singleDeviceModelId(61L).enabled(true).build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(rentalOrderMapper.selectByIdForUpdate(31L)).thenReturn(existingOrder);
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(31L)).thenReturn(existingItem);
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1")).thenReturn(rule);
        when(modelMapper.selectById(61L)).thenReturn(RentalDeviceModelDO.builder()
                .id(61L).modelCode("A7M4").enabled(true).build());

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("READY", result.preparationStatus());
        assertSame(existingOrder, capturedUpdatedOrder());
        assertEquals("A7M4", existingItem.getEquipmentModelCode());
        assertEquals(LocalDate.of(2026, 7, 22), existingItem.getOccupyStartDate());
        assertEquals(LocalDate.of(2026, 7, 28), existingItem.getOccupyEndDateExclusive());
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).insert(any(RentalOrderItemDO.class));
        verify(skuMappingMapper, never()).selectEnabledByRuleIdAndXgjSkuIdForUpdate(any(), any());
    }

    @Test
    void linksExistingSourceOrderAsUpdatedInsteadOfCreated() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalOrderId(null);
        source.setPreparationStatus("WAITING_MODEL");
        RentalOrderDO existingOrder = RentalOrderDO.builder()
                .id(31L)
                .sourceType("XIANYU")
                .sourceOrderId("7:order-1")
                .channelOrderId(10L)
                .status("PENDING_ALLOCATION")
                .preparationStatus("WAITING_MODEL")
                .build();
        RentalOrderItemDO existingItem = RentalOrderItemDO.builder()
                .id(41L)
                .rentalOrderId(31L)
                .quantity(1)
                .rentAmount(source.getPayAmount())
                .build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(rentalOrderMapper.selectBySourceForUpdate("XIANYU", "7:order-1"))
                .thenReturn(existingOrder);
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(31L))
                .thenReturn(existingItem);

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("UPDATED", result.mutationKind());
        assertEquals(31L, source.getRentalOrderId());
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).insert(any(RentalOrderItemDO.class));
    }

    @Test
    void skipsConfiguredProductWithoutRemarkReviewOrInternalOrder() {
        XianyuOrderDO source = sourceOrder();
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L).handlingPolicy("CONFIG_SKIPPED").mappingMode("NONE").enabled(true).build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1")).thenReturn(rule);

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("CONFIG_SKIPPED", result.status());
        assertEquals("CONFIG_SKIPPED", source.getPreparationStatus());
        assertEquals("CONFIG_SKIPPED", source.getConversionStatus());
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).insert(any(RentalOrderItemDO.class));
        verify(manualReviewMapper, never()).insert(any(RentalManualReviewDO.class));
    }

    @Test
    void multiModelRequiresExactXgjSkuAndNeverFallsBackToProductModel() {
        XianyuOrderDO source = sourceOrder();
        successfulPeriod(source);
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L).handlingPolicy("CREATE_RENTAL").mappingMode("MULTI")
                .singleDeviceModelId(999L).enabled(true).build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1")).thenReturn(rule);
        when(skuMappingMapper.selectEnabledByRuleIdAndXgjSkuIdForUpdate(51L, "sku-1")).thenReturn(null);
        assignGeneratedIds();

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("WAITING_MODEL", result.preparationStatus());
        assertEquals("SKU_MODEL_NOT_CONFIGURED", result.reasonCode());
        verify(modelMapper, never()).selectById(999L);
        ArgumentCaptor<RentalOrderItemDO> itemCaptor = ArgumentCaptor.forClass(RentalOrderItemDO.class);
        verify(rentalOrderItemMapper).insert(itemCaptor.capture());
        assertNull(itemCaptor.getValue().getEquipmentModelCode());
    }

    @Test
    void multiModelUsesOnlyTheExactEnabledSkuMapping() {
        XianyuOrderDO source = sourceOrder();
        successfulPeriod(source);
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L).handlingPolicy("CREATE_RENTAL").mappingMode("MULTI").enabled(true).build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1")).thenReturn(rule);
        when(skuMappingMapper.selectEnabledByRuleIdAndXgjSkuIdForUpdate(51L, "sku-1"))
                .thenReturn(RentalChannelProductSkuMappingDO.builder()
                        .deviceModelId(61L).xgjSkuId("sku-1").enabled(true).build());
        when(modelMapper.selectById(61L)).thenReturn(RentalDeviceModelDO.builder()
                .id(61L).modelCode("DJI-P4P").enabled(true).build());
        AtomicReference<ItemInsertSnapshot> insertedItem = captureGeneratedItemAtInsert();

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("READY", result.preparationStatus());
        assertEquals("DJI-P4P", insertedItem.get().equipmentModelCode());
        assertEquals(LocalDate.of(2026, 7, 25), insertedItem.get().billableStartDate());
        assertEquals(LocalDate.of(2026, 7, 27), insertedItem.get().billableEndDate());
        assertEquals(LocalDate.of(2026, 7, 22), insertedItem.get().occupyStartDate());
        assertEquals(LocalDate.of(2026, 7, 28), insertedItem.get().occupyEndDateExclusive());
        verify(skuMappingMapper).selectEnabledByRuleIdAndXgjSkuIdForUpdate(51L, "sku-1");
    }

    @Test
    void configuredModelWithoutRemarkWaitsForRemarkAndPersistsModelImmediately() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalPeriodStatus("PENDING");
        source.setRentalPeriodReasonCode("MISSING_REMARK");
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L).handlingPolicy("CREATE_RENTAL").mappingMode("SINGLE")
                .singleDeviceModelId(61L).enabled(true).build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1")).thenReturn(rule);
        when(modelMapper.selectById(61L)).thenReturn(RentalDeviceModelDO.builder()
                .id(61L).modelCode("A7M4").enabled(true).build());
        AtomicReference<ItemInsertSnapshot> insertedItem = captureGeneratedItemAtInsert();

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("WAITING_REMARK", result.preparationStatus());
        assertEquals("MISSING_REMARK", result.reasonCode());
        assertEquals("A7M4", insertedItem.get().equipmentModelCode());
        assertNull(insertedItem.get().billableStartDate());
        assertNull(insertedItem.get().occupyStartDate());
    }

    @Test
    void configurationSkippedLookupIsExactAndDisabledRulesDoNotMatch() {
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1"))
                .thenReturn(RentalChannelProductRuleDO.builder()
                        .handlingPolicy("CONFIG_SKIPPED").enabled(false).build());

        assertEquals(false, service.isConfigurationSkipped(7L, "item-1", false));
        assertEquals(false, service.isConfigurationSkipped(7L, "item-1", true));

        verify(ruleMapper).selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1");
    }

    @Test
    void skipRuleNeverClearsAnExistingInternalOrderModel() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalOrderId(31L);
        successfulPeriod(source);
        RentalOrderDO existingOrder = RentalOrderDO.builder()
                .id(31L)
                .sourceType("XIANYU")
                .sourceOrderId("7:order-1")
                .channelOrderId(10L)
                .status("PENDING_ALLOCATION")
                .preparationStatus("READY")
                .build();
        RentalOrderItemDO existingItem = RentalOrderItemDO.builder()
                .id(41L)
                .rentalOrderId(31L)
                .equipmentModelCode("A7M4")
                .quantity(1)
                .rentAmount(source.getPayAmount())
                .billableStartDate(LocalDate.of(2026, 7, 25))
                .billableEndDate(LocalDate.of(2026, 7, 27))
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 28))
                .build();
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L).handlingPolicy("CONFIG_SKIPPED").mappingMode("NONE").enabled(true).build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(rentalOrderMapper.selectByIdForUpdate(31L)).thenReturn(existingOrder);
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(31L)).thenReturn(existingItem);
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1")).thenReturn(rule);

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("READY", result.preparationStatus());
        assertEquals("A7M4", existingItem.getEquipmentModelCode());
        verify(assignmentMapper, never()).countAssignedByOrderItem(41L);
    }

    @Test
    void derivesXianyuSkuOnlyWhenTheSynchronizedXgjProductAlsoMatches() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalPeriodStatus("PENDING");
        source.setRentalPeriodReasonCode("MISSING_REMARK");
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(productMapper.selectByShopIdAndXianyuItemId(7L, "item-1"))
                .thenReturn(XianyuProductDO.builder()
                        .id(71L).xgjProductId("different-product").xianyuItemId("item-1").build());
        assignGeneratedIds();

        service.reconcile(10L);

        assertNull(source.getXianyuSkuId());
        verify(productSkuMapper, never()).selectByProductIdAndXgjSkuIdForUpdate(any(), any());
    }

    @Test
    void derivesXianyuSkuFromTheCompleteSynchronizedProductRelationship() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalPeriodStatus("PENDING");
        source.setRentalPeriodReasonCode("MISSING_REMARK");
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(productMapper.selectByShopIdAndXianyuItemId(7L, "item-1"))
                .thenReturn(XianyuProductDO.builder()
                        .id(71L).xgjProductId("product-1").xianyuItemId("item-1").build());
        when(productSkuMapper.selectByProductIdAndXgjSkuIdForUpdate(71L, "sku-1"))
                .thenReturn(XianyuProductSkuDO.builder()
                        .productId(71L).xgjSkuId("sku-1").xianyuSkuId("xy-sku-1").build());
        assignGeneratedIds();

        service.reconcile(10L);

        assertEquals("xy-sku-1", source.getXianyuSkuId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"11", "23", "24"})
    void ineligibleOrdersRetainChannelEvidenceWithoutCreatingInternalOrders(String orderStatus) {
        XianyuOrderDO source = sourceOrder();
        source.setOrderStatus(orderStatus);
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("INELIGIBLE", result.status());
        assertEquals("INELIGIBLE", source.getPreparationStatus());
        assertEquals("INELIGIBLE", source.getConversionStatus());
        assertNull(source.getRentalOrderId());
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).insert(any(RentalOrderItemDO.class));
        verify(manualReviewMapper, never()).insert(any(RentalManualReviewDO.class));
    }

    @Test
    void successfulRefundStatusIsIneligibleEvenWhenOrderStatusWasPaid() {
        XianyuOrderDO source = sourceOrder();
        source.setRefundStatus(5);
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("ORDER_REFUNDED", result.reasonCode());
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
    }

    @Test
    void mismatchedLinkedRentalOrderIsNeverMutated() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalOrderId(31L);
        RentalOrderDO unrelated = RentalOrderDO.builder()
                .id(31L)
                .sourceType("XIANYU")
                .sourceOrderId("7:another-order")
                .channelOrderId(999L)
                .build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(rentalOrderMapper.selectByIdForUpdate(31L)).thenReturn(unrelated);

        RentalChannelOrderReconciliationResult result = service.reconcile(10L);

        assertEquals("REVIEW_REQUIRED", result.status());
        assertEquals("RENTAL_ORDER_LINK_CONFLICT", result.reasonCode());
        verify(rentalOrderMapper, never()).updateById(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).selectFirstByRentalOrderIdForUpdate(any());
        verify(manualReviewMapper).insert(any(RentalManualReviewDO.class));
    }

    @Test
    void resolvedReviewIsReopenedWhenTheConflictReturns() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalOrderId(31L);
        RentalOrderDO unrelated = RentalOrderDO.builder()
                .id(31L).sourceType("XIANYU").sourceOrderId("7:other").channelOrderId(999L).build();
        RentalManualReviewDO review = RentalManualReviewDO.builder()
                .id(81L)
                .status("RESOLVED")
                .reasonCode("OLD_REASON")
                .resolutionNote("previously resolved")
                .resolvedBy(5L)
                .resolvedAt(java.time.LocalDateTime.of(2026, 8, 30, 10, 0))
                .build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(rentalOrderMapper.selectByIdForUpdate(31L)).thenReturn(unrelated);
        when(manualReviewMapper.selectBySourceAndReviewTypeForUpdate(
                "XIANYU_ORDER", "10", "ORDER_CONVERSION")).thenReturn(review);

        service.reconcile(10L);

        assertEquals("OPEN", review.getStatus());
        assertEquals("RENTAL_ORDER_LINK_CONFLICT", review.getReasonCode());
        assertNull(review.getResolutionNote());
        assertNull(review.getResolvedBy());
        assertNull(review.getResolvedAt());
        verify(manualReviewMapper).updateById(review);
    }

    @Test
    void newManualReviewInheritsChannelOrderTenant() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalOrderId(31L);
        RentalOrderDO unrelated = RentalOrderDO.builder()
                .id(31L).sourceType("XIANYU").sourceOrderId("7:other").channelOrderId(999L).build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(rentalOrderMapper.selectByIdForUpdate(31L)).thenReturn(unrelated);

        service.reconcile(10L);

        ArgumentCaptor<RentalManualReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(RentalManualReviewDO.class);
        verify(manualReviewMapper).insert(reviewCaptor.capture());
        assertEquals(9L, reviewCaptor.getValue().getTenantId());
    }

    @Test
    void statefulRetryCreatesExactlyOneOrderAndOneItem() {
        XianyuOrderDO source = sourceOrder();
        successfulPeriod(source);
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L).handlingPolicy("CREATE_RENTAL").mappingMode("SINGLE")
                .singleDeviceModelId(61L).enabled(true).build();
        AtomicReference<RentalOrderDO> storedOrder = new AtomicReference<>();
        AtomicReference<RentalOrderItemDO> storedItem = new AtomicReference<>();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(7L, "item-1")).thenReturn(rule);
        when(modelMapper.selectById(61L)).thenReturn(RentalDeviceModelDO.builder()
                .id(61L).modelCode("A7M4").enabled(true).build());
        when(rentalOrderMapper.selectBySourceForUpdate("XIANYU", "7:order-1"))
                .thenAnswer(invocation -> storedOrder.get());
        when(rentalOrderMapper.selectByIdForUpdate(31L))
                .thenAnswer(invocation -> storedOrder.get());
        when(rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(31L))
                .thenAnswer(invocation -> storedItem.get());
        doAnswer(invocation -> {
            RentalOrderDO order = invocation.getArgument(0, RentalOrderDO.class);
            order.setId(31L);
            storedOrder.set(order);
            return 1;
        }).when(rentalOrderMapper).insert(any(RentalOrderDO.class));
        doAnswer(invocation -> {
            RentalOrderItemDO item = invocation.getArgument(0, RentalOrderItemDO.class);
            item.setId(41L);
            storedItem.set(item);
            return 1;
        }).when(rentalOrderItemMapper).insert(any(RentalOrderItemDO.class));

        RentalChannelOrderReconciliationResult first = service.reconcile(10L);
        RentalChannelOrderReconciliationResult second = service.reconcile(10L);

        assertEquals("CREATED", first.mutationKind());
        assertEquals("UNCHANGED", second.mutationKind());
        assertEquals(31L, first.rentalOrderId());
        assertEquals(31L, second.rentalOrderId());
        assertSame(storedOrder.get(), rentalOrderMapper.selectByIdForUpdate(31L));
        assertEquals("A7M4", storedItem.get().getEquipmentModelCode());
        verify(rentalOrderMapper, times(1)).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, times(1)).insert(any(RentalOrderItemDO.class));
    }

    @Test
    void resolvesDisplayModelCodeFromSingleModeRule() {
        XianyuOrderDO order = sourceOrder();
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(51L)
                .shopId(7L)
                .xianyuItemId("item-1")
                .handlingPolicy("CREATE_RENTAL")
                .mappingMode("SINGLE")
                .singleDeviceModelId(61L)
                .enabled(true)
                .build();
        when(ruleMapper.selectEnabledListByXianyuItemIds(List.of("item-1")))
                .thenReturn(List.of(rule));
        when(modelMapper.selectByIds(List.of(61L))).thenReturn(List.of(
                RentalDeviceModelDO.builder().id(61L).modelCode("POCKET4").enabled(true).build()));

        Map<Long, String> result = service.resolveDisplayModelCodes(List.of(order));

        assertEquals(Map.of(10L, "POCKET4"), result);
    }

    @Test
    void resolvesDisplayModelCodeFromMultiModeSkuMapping() {
        XianyuOrderDO order = sourceOrder();
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(52L)
                .shopId(7L)
                .xianyuItemId("item-1")
                .handlingPolicy("CREATE_RENTAL")
                .mappingMode("MULTI")
                .enabled(true)
                .build();
        when(ruleMapper.selectEnabledListByXianyuItemIds(List.of("item-1")))
                .thenReturn(List.of(rule));
        when(skuMappingMapper.selectListByProductRuleIds(List.of(52L))).thenReturn(List.of(
                RentalChannelProductSkuMappingDO.builder()
                        .id(71L)
                        .productRuleId(52L)
                        .xgjSkuId("sku-1")
                        .deviceModelId(62L)
                        .enabled(true)
                        .build()));
        when(modelMapper.selectByIds(List.of(62L))).thenReturn(List.of(
                RentalDeviceModelDO.builder().id(62L).modelCode("A7M4").enabled(true).build()));

        Map<Long, String> result = service.resolveDisplayModelCodes(List.of(order));

        assertEquals(Map.of(10L, "A7M4"), result);
    }

    @Test
    void skipsDisplayModelCodeForSkippedPolicyDisabledModelAndMissingSku() {
        XianyuOrderDO skippedPolicyOrder = sourceOrder();
        RentalChannelProductRuleDO skippedRule = RentalChannelProductRuleDO.builder()
                .id(53L)
                .shopId(7L)
                .xianyuItemId("item-1")
                .handlingPolicy("CONFIG_SKIPPED")
                .mappingMode("SINGLE")
                .singleDeviceModelId(61L)
                .enabled(true)
                .build();
        when(ruleMapper.selectEnabledListByXianyuItemIds(List.of("item-1")))
                .thenReturn(List.of(skippedRule));

        assertTrue(service.resolveDisplayModelCodes(List.of(skippedPolicyOrder)).isEmpty());

        XianyuOrderDO disabledModelOrder = sourceOrder();
        RentalChannelProductRuleDO singleRule = RentalChannelProductRuleDO.builder()
                .id(54L)
                .shopId(7L)
                .xianyuItemId("item-1")
                .handlingPolicy("CREATE_RENTAL")
                .mappingMode("SINGLE")
                .singleDeviceModelId(61L)
                .enabled(true)
                .build();
        when(ruleMapper.selectEnabledListByXianyuItemIds(List.of("item-1")))
                .thenReturn(List.of(singleRule));
        when(modelMapper.selectByIds(List.of(61L))).thenReturn(List.of(
                RentalDeviceModelDO.builder().id(61L).modelCode("POCKET4").enabled(false).build()));

        assertTrue(service.resolveDisplayModelCodes(List.of(disabledModelOrder)).isEmpty());

        XianyuOrderDO missingSkuOrder = sourceOrder();
        missingSkuOrder.setXgjSkuId("sku-unknown");
        RentalChannelProductRuleDO multiRule = RentalChannelProductRuleDO.builder()
                .id(55L)
                .shopId(7L)
                .xianyuItemId("item-1")
                .handlingPolicy("CREATE_RENTAL")
                .mappingMode("MULTI")
                .enabled(true)
                .build();
        when(ruleMapper.selectEnabledListByXianyuItemIds(List.of("item-1")))
                .thenReturn(List.of(multiRule));
        when(skuMappingMapper.selectListByProductRuleIds(List.of(55L))).thenReturn(List.of(
                RentalChannelProductSkuMappingDO.builder()
                        .id(72L)
                        .productRuleId(55L)
                        .xgjSkuId("sku-1")
                        .deviceModelId(62L)
                        .enabled(true)
                        .build()));

        assertTrue(service.resolveDisplayModelCodes(List.of(missingSkuOrder)).isEmpty());
    }

    @Test
    void resolveDisplayModelCodesSkipsOrdersWithoutXianyuItemId() {
        XianyuOrderDO order = sourceOrder();
        order.setXianyuItemId(" ");

        assertTrue(service.resolveDisplayModelCodes(List.of(order)).isEmpty());
        verify(ruleMapper, never()).selectEnabledListByXianyuItemIds(any());
    }

    private RentalOrderDO capturedUpdatedOrder() {
        ArgumentCaptor<RentalOrderDO> captor = ArgumentCaptor.forClass(RentalOrderDO.class);
        verify(rentalOrderMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private void assignGeneratedIds() {
        doAnswer(invocation -> {
            invocation.getArgument(0, RentalOrderDO.class).setId(31L);
            return 1;
        }).when(rentalOrderMapper).insert(any(RentalOrderDO.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, RentalOrderItemDO.class).setId(41L);
            return 1;
        }).when(rentalOrderItemMapper).insert(any(RentalOrderItemDO.class));
    }

    private AtomicReference<ItemInsertSnapshot> captureGeneratedItemAtInsert() {
        AtomicReference<ItemInsertSnapshot> snapshot = new AtomicReference<>();
        doAnswer(invocation -> {
            invocation.getArgument(0, RentalOrderDO.class).setId(31L);
            return 1;
        }).when(rentalOrderMapper).insert(any(RentalOrderDO.class));
        doAnswer(invocation -> {
            RentalOrderItemDO item = invocation.getArgument(0, RentalOrderItemDO.class);
            snapshot.set(new ItemInsertSnapshot(
                    item.getEquipmentModelCode(),
                    item.getBillableStartDate(),
                    item.getBillableEndDate(),
                    item.getOccupyStartDate(),
                    item.getOccupyEndDateExclusive()));
            item.setId(41L);
            return 1;
        }).when(rentalOrderItemMapper).insert(any(RentalOrderItemDO.class));
        return snapshot;
    }

    private XianyuOrderDO sourceOrder() {
        XianyuOrderDO source = XianyuOrderDO.builder()
                .id(10L)
                .shopId(7L)
                .externalOrderId("order-1")
                .xgjProductId("product-1")
                .xianyuItemId("item-1")
                .xgjSkuId("sku-1")
                .orderStatus("12")
                .payAmount(4_294_967_296L)
                .goodsQuantity(1)
                .conversionStatus("PENDING")
                .build();
        source.setTenantId(9L);
        return source;
    }

    private void successfulPeriod(XianyuOrderDO source) {
        source.setSellerRemark("发货7.22/收货7.24/租期7.25-7.27/发回7.27");
        source.setRemarkParseVersion("SELLER_REMARK_V6");
        source.setRentalPeriodStatus("SUCCESS");
        source.setBillableStartDate(LocalDate.of(2026, 7, 25));
        source.setBillableEndDate(LocalDate.of(2026, 7, 27));
        source.setShipDate(LocalDate.of(2026, 7, 22));
        source.setReceiveDate(LocalDate.of(2026, 7, 24));
        source.setReturnDate(LocalDate.of(2026, 7, 27));
    }

    private record ItemInsertSnapshot(
            String equipmentModelCode,
            LocalDate billableStartDate,
            LocalDate billableEndDate,
            LocalDate occupyStartDate,
            LocalDate occupyEndDateExclusive) {
    }

}
