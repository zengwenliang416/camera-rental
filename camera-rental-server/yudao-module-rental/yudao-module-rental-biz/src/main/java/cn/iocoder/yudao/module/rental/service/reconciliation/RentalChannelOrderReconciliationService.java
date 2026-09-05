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
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductSkuMapper;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.rental.RentalManualReviewStatusEnum.OPEN;
import static cn.iocoder.yudao.module.rental.enums.rental.RentalManualReviewStatusEnum.RESOLVED;

@Service
@Slf4j
public class RentalChannelOrderReconciliationService {

    private static final String CHANNEL_SOURCE_TYPE = "XIANYU";
    private static final String REVIEW_SOURCE_TYPE = "XIANYU_ORDER";
    private static final String REVIEW_TYPE = "ORDER_CONVERSION";
    private static final String FULFILLMENT_REVIEW_TYPE = "FULFILLMENT_UPDATE";
    private static final String CONVERSION_STATUS_CONVERTED = "CONVERTED";
    private static final String CONVERSION_STATUS_CLOSED = "CLOSED";
    private static final String CONVERSION_STATUS_REVIEW_REQUIRED = "REVIEW_REQUIRED";
    private static final String HANDLING_POLICY_SKIPPED = "CONFIG_SKIPPED";
    private static final String RENTAL_STATUS_PENDING_ALLOCATION = "PENDING_ALLOCATION";
    private static final String SYSTEM_OPERATOR = "system";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuOrderMapper xianyuOrderMapper;
    private final XianyuProductMapper productMapper;
    private final XianyuProductSkuMapper productSkuMapper;
    private final RentalChannelProductRuleMapper ruleMapper;
    private final RentalChannelProductSkuMappingMapper skuMappingMapper;
    private final RentalDeviceModelMapper modelMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final RentalOrderItemMapper rentalOrderItemMapper;
    private final RentalManualReviewMapper manualReviewMapper;
    private final RentalChannelOrderEligibilityPolicy eligibilityPolicy;
    private final RentalOrderPreparationPolicy preparationPolicy;
    private final RentalFulfillmentUpdateGuard fulfillmentUpdateGuard;
    private final RentalRemarkPlanChangeClassifier remarkChangeClassifier;

    public RentalChannelOrderReconciliationService(
            XianyuOrderMapper xianyuOrderMapper,
            XianyuProductMapper productMapper,
            XianyuProductSkuMapper productSkuMapper,
            RentalChannelProductRuleMapper ruleMapper,
            RentalChannelProductSkuMappingMapper skuMappingMapper,
            RentalDeviceModelMapper modelMapper,
            RentalOrderMapper rentalOrderMapper,
            RentalOrderItemMapper rentalOrderItemMapper,
            RentalManualReviewMapper manualReviewMapper,
            RentalChannelOrderEligibilityPolicy eligibilityPolicy,
            RentalOrderPreparationPolicy preparationPolicy,
            RentalFulfillmentUpdateGuard fulfillmentUpdateGuard,
            RentalRemarkPlanChangeClassifier remarkChangeClassifier) {
        this.xianyuOrderMapper = xianyuOrderMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.ruleMapper = ruleMapper;
        this.skuMappingMapper = skuMappingMapper;
        this.modelMapper = modelMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.rentalOrderItemMapper = rentalOrderItemMapper;
        this.manualReviewMapper = manualReviewMapper;
        this.eligibilityPolicy = eligibilityPolicy;
        this.preparationPolicy = preparationPolicy;
        this.fulfillmentUpdateGuard = fulfillmentUpdateGuard;
        this.remarkChangeClassifier = remarkChangeClassifier;
    }

    public boolean isConfigurationSkipped(Long shopId, String xianyuItemId, boolean hasInternalOrder) {
        if (shopId == null || !StringUtils.hasText(xianyuItemId) || hasInternalOrder) {
            return false;
        }
        RentalChannelProductRuleDO rule =
                ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(shopId, xianyuItemId.trim());
        return isSkipped(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalChannelOrderReconciliationResult reconcile(Long channelOrderId) {
        return reconcileLocked(channelOrderId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalChannelOrderReconciliationResult reconcile(
            Long channelOrderId, RentalRemarkPlanUpdate requestedPlanUpdate) {
        return reconcileLocked(channelOrderId, requestedPlanUpdate);
    }

    private RentalChannelOrderReconciliationResult reconcileLocked(
            Long channelOrderId, RentalRemarkPlanUpdate requestedPlanUpdate) {
        Objects.requireNonNull(channelOrderId, "channelOrderId");
        XianyuOrderDO source = xianyuOrderMapper.selectByIdForUpdate(channelOrderId);
        if (source == null) {
            throw exception(XIANYU_ORDER_NOT_EXISTS);
        }
        RentalOrderDO rentalOrder = findExistingRentalOrder(source);
        if (rentalOrder != null && !belongsToSource(rentalOrder, source)) {
            return requireReview(source, "RENTAL_ORDER_LINK_CONFLICT");
        }
        if (CONVERSION_STATUS_CLOSED.equals(source.getConversionStatus()) && rentalOrder == null) {
            return RentalChannelOrderReconciliationResult.reviewRequired(null, "CLOSED");
        }

        RentalChannelProductRuleDO rule = findEnabledRule(source);
        if (isSkipped(rule) && rentalOrder == null) {
            markConfigurationSkipped(source);
            resolveConversionReview(source.getId(), "Configured product skipped");
            return RentalChannelOrderReconciliationResult.skipped();
        }
        String ineligibleReason = eligibilityPolicy.ineligibleReason(source);
        if (rentalOrder == null && ineligibleReason != null) {
            markIneligible(source, ineligibleReason);
            resolveConversionReview(source.getId(), "Channel order is not eligible for rental creation");
            return RentalChannelOrderReconciliationResult.ineligible(ineligibleReason);
        }

        boolean createdOrder = rentalOrder == null;
        SourceOrderSemanticState beforeState = createdOrder
                ? null : SourceOrderSemanticState.capture(source, rentalOrder);
        if (createdOrder) {
            rentalOrder = createRentalOrder(source);
        } else if (source.getRentalOrderId() == null) {
            source.setRentalOrderId(rentalOrder.getId());
        }
        enrichExactXianyuSku(source);
        ModelResolution modelResolution = resolveModel(source, rule);
        RentalOrderItemDO item = rentalOrderItemMapper.selectFirstByRentalOrderIdForUpdate(rentalOrder.getId());
        boolean createdItem = item == null;
        if (createdItem) {
            item = buildRentalOrderItem(source, rentalOrder.getId());
        }
        ItemSemanticState beforeItemState = createdItem
                ? null : ItemSemanticState.capture(item);
        RentalRemarkPlanUpdate planUpdate = requestedPlanUpdate == null
                ? currentPlanUpdate(source, item) : requestedPlanUpdate;
        RentalFulfillmentUpdateResult fulfillmentUpdate = isSkipped(rule)
                ? RentalFulfillmentUpdateResult.applied(false, false)
                : fulfillmentUpdateGuard.apply(rentalOrder, item, planUpdate, modelResolution.modelCode());
        if (createdItem) {
            rentalOrderItemMapper.insert(item);
        }
        if (fulfillmentUpdate.reviewRequired()) {
            source.setRentalOrderId(rentalOrder.getId());
            return requireFulfillmentReview(source, fulfillmentUpdate.reasonCode());
        }
        item.setSourceProductId(trimToNull(source.getXianyuItemId()));
        item.setSourceSkuId(trimToNull(source.getXgjSkuId()));

        RentalOrderPreparationDecision preparation =
                preparationPolicy.evaluate(source, item, modelResolution.reasonCode());
        LocalDateTime updatedAt = LocalDateTime.now(BUSINESS_ZONE);
        applyPreparation(source, rentalOrder, preparation, updatedAt);
        source.setRentalOrderId(rentalOrder.getId());
        source.setConversionStatus(CONVERSION_STATUS_CONVERTED);
        source.setUpdater(SYSTEM_OPERATOR);
        xianyuOrderMapper.updateById(source);
        rentalOrder.setUpdater(SYSTEM_OPERATOR);
        rentalOrderMapper.updateById(rentalOrder);
        if (!createdItem) {
            item.setUpdater(SYSTEM_OPERATOR);
            rentalOrderItemMapper.updateById(item);
        }
        resolveConversionReview(source.getId(), "Internal rental order created");
        log.info("[xianyu][reconcile] channelOrderId={} rentalOrderId={} preparation={}",
                source.getId(), rentalOrder.getId(), preparation.status());
        String mutationKind = createdOrder ? "CREATED" : changedExistingOrder(
                beforeState, beforeItemState, source, rentalOrder, item, createdItem)
                ? "UPDATED" : "UNCHANGED";
        return RentalChannelOrderReconciliationResult.converted(
                rentalOrder.getId(), preparation, fulfillmentUpdate.planApplied(), mutationKind);
    }

    private static boolean changedExistingOrder(
            SourceOrderSemanticState beforeState,
            ItemSemanticState beforeItemState,
            XianyuOrderDO source,
            RentalOrderDO rentalOrder,
            RentalOrderItemDO item,
            boolean createdItem) {
        return createdItem
                || beforeState.differsFrom(source, rentalOrder)
                || beforeItemState.differsFrom(item);
    }

    private RentalRemarkPlanUpdate currentPlanUpdate(XianyuOrderDO source, RentalOrderItemDO item) {
        SellerRemarkRentalPeriod previous = currentPlan(item);
        SellerRemarkRentalPeriod candidate = candidatePlan(source);
        return new RentalRemarkPlanUpdate(previous, candidate,
                remarkChangeClassifier.classify(source.getSellerRemark(), previous, candidate));
    }

    private static SellerRemarkRentalPeriod candidatePlan(XianyuOrderDO source) {
        if (source == null || !"SUCCESS".equals(source.getRentalPeriodStatus())
                || source.getBillableStartDate() == null || source.getBillableEndDate() == null
                || source.getShipDate() == null || source.getReturnDate() == null) {
            return SellerRemarkRentalPeriod.pending(
                    source == null || source.getRemarkParseVersion() == null
                            ? "UNKNOWN" : source.getRemarkParseVersion(),
                    source == null ? "MISSING_SOURCE" : source.getRentalPeriodReasonCode());
        }
        return SellerRemarkRentalPeriod.success(
                source.getRemarkParseVersion(),
                source.getBillableStartDate(),
                source.getBillableEndDate(),
                source.getShipDate(),
                source.getReceiveDate(),
                source.getReturnDate());
    }

    private static SellerRemarkRentalPeriod currentPlan(RentalOrderItemDO item) {
        if (item.getBillableStartDate() == null || item.getBillableEndDate() == null
                || item.getOccupyStartDate() == null || item.getOccupyEndDateExclusive() == null) {
            return null;
        }
        LocalDate returnDate = item.getOccupyEndDateExclusive().minusDays(1);
        return SellerRemarkRentalPeriod.success(
                "EFFECTIVE_INTERNAL_PLAN",
                item.getBillableStartDate(),
                item.getBillableEndDate(),
                item.getOccupyStartDate(),
                item.getBillableStartDate().minusDays(1),
                returnDate);
    }

    private RentalOrderDO findExistingRentalOrder(XianyuOrderDO source) {
        RentalOrderDO linked = source.getRentalOrderId() == null
                ? null : rentalOrderMapper.selectByIdForUpdate(source.getRentalOrderId());
        if (linked != null) {
            return linked;
        }
        return rentalOrderMapper.selectBySourceForUpdate(CHANNEL_SOURCE_TYPE, sourceIdentity(source));
    }

    private boolean belongsToSource(RentalOrderDO rentalOrder, XianyuOrderDO source) {
        return CHANNEL_SOURCE_TYPE.equals(rentalOrder.getSourceType())
                && sourceIdentity(source).equals(rentalOrder.getSourceOrderId())
                && Objects.equals(source.getId(), rentalOrder.getChannelOrderId());
    }

    private RentalOrderDO createRentalOrder(XianyuOrderDO source) {
        RentalOrderDO rentalOrder = RentalOrderDO.builder()
                .orderNo("XY-" + String.format("%019d", source.getId()))
                .sourceType(CHANNEL_SOURCE_TYPE)
                .sourceOrderId(sourceIdentity(source))
                .channelOrderId(source.getId())
                .status(RENTAL_STATUS_PENDING_ALLOCATION)
                .rentAmount(source.getPayAmount())
                .refundAmount(0L)
                .preparationStatus("WAITING_RECONCILIATION")
                .build();
        rentalOrder.setTenantId(source.getTenantId());
        rentalOrder.setCreator(SYSTEM_OPERATOR);
        rentalOrder.setUpdater(SYSTEM_OPERATOR);
        rentalOrderMapper.insert(rentalOrder);
        return rentalOrder;
    }

    private RentalOrderItemDO buildRentalOrderItem(XianyuOrderDO source, Long rentalOrderId) {
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .rentalOrderId(rentalOrderId)
                .sourceProductId(trimToNull(source.getXianyuItemId()))
                .sourceSkuId(trimToNull(source.getXgjSkuId()))
                .quantity(source.getGoodsQuantity() == null || source.getGoodsQuantity() < 1
                        ? 1 : source.getGoodsQuantity())
                .rentAmount(source.getPayAmount())
                .build();
        item.setTenantId(source.getTenantId());
        item.setCreator(SYSTEM_OPERATOR);
        item.setUpdater(SYSTEM_OPERATOR);
        return item;
    }

    private RentalChannelProductRuleDO findEnabledRule(XianyuOrderDO source) {
        if (!StringUtils.hasText(source.getXianyuItemId())) {
            return null;
        }
        return ruleMapper.selectEnabledByShopIdAndItemIdForUpdate(
                source.getShopId(), source.getXianyuItemId().trim());
    }

    private ModelResolution resolveModel(XianyuOrderDO source, RentalChannelProductRuleDO rule) {
        if (!StringUtils.hasText(source.getXianyuItemId())) {
            return ModelResolution.missing("MISSING_XIANYU_ITEM_ID");
        }
        if (rule == null || !"CREATE_RENTAL".equals(rule.getHandlingPolicy())) {
            return ModelResolution.missing("PRODUCT_RULE_NOT_CONFIGURED");
        }
        if ("SINGLE".equals(rule.getMappingMode())) {
            return modelResolution(rule.getSingleDeviceModelId(), "SINGLE_MODEL_NOT_CONFIGURED");
        }
        if (!"MULTI".equals(rule.getMappingMode())) {
            return ModelResolution.missing("MODEL_MAPPING_MODE_INVALID");
        }
        if (!StringUtils.hasText(source.getXgjSkuId())) {
            return ModelResolution.missing("MISSING_XGJ_SKU_ID");
        }
        RentalChannelProductSkuMappingDO mapping =
                skuMappingMapper.selectEnabledByRuleIdAndXgjSkuIdForUpdate(
                        rule.getId(), source.getXgjSkuId().trim());
        return mapping == null
                ? ModelResolution.missing("SKU_MODEL_NOT_CONFIGURED")
                : modelResolution(mapping.getDeviceModelId(), "SKU_MODEL_NOT_CONFIGURED");
    }

    private ModelResolution modelResolution(Long modelId, String missingReason) {
        RentalDeviceModelDO model = modelId == null ? null : modelMapper.selectById(modelId);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())
                || !StringUtils.hasText(model.getModelCode())) {
            return ModelResolution.missing(missingReason);
        }
        return new ModelResolution(model.getModelCode().trim(), null);
    }

    /**
     * Read-only batch variant of {@link #resolveModel} for list display: resolves the configured
     * equipment model code for orders that have no rental order item yet. No row locks, no N+1.
     */
    public Map<Long, String> resolveDisplayModelCodes(List<XianyuOrderDO> orders) {
        if (orders == null || orders.isEmpty()) {
            return Map.of();
        }
        List<String> itemIds = orders.stream()
                .filter(order -> StringUtils.hasText(order.getXianyuItemId()))
                .map(order -> order.getXianyuItemId().trim())
                .distinct()
                .toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<String, RentalChannelProductRuleDO> ruleByShopAndItem = new HashMap<>();
        for (RentalChannelProductRuleDO rule : ruleMapper.selectEnabledListByXianyuItemIds(itemIds)) {
            if (!"CREATE_RENTAL".equals(rule.getHandlingPolicy())
                    || !StringUtils.hasText(rule.getXianyuItemId())) {
                continue;
            }
            ruleByShopAndItem.putIfAbsent(shopItemKey(rule.getShopId(), rule.getXianyuItemId()), rule);
        }
        if (ruleByShopAndItem.isEmpty()) {
            return Map.of();
        }
        List<Long> multiRuleIds = ruleByShopAndItem.values().stream()
                .filter(rule -> "MULTI".equals(rule.getMappingMode()))
                .map(RentalChannelProductRuleDO::getId)
                .distinct()
                .toList();
        Map<String, Long> modelIdByRuleAndSku = new HashMap<>();
        if (!multiRuleIds.isEmpty()) {
            for (RentalChannelProductSkuMappingDO mapping
                    : skuMappingMapper.selectListByProductRuleIds(multiRuleIds)) {
                if (!Boolean.TRUE.equals(mapping.getEnabled())
                        || !StringUtils.hasText(mapping.getXgjSkuId())) {
                    continue;
                }
                modelIdByRuleAndSku.putIfAbsent(
                        mapping.getProductRuleId() + "|" + mapping.getXgjSkuId().trim(),
                        mapping.getDeviceModelId());
            }
        }
        Map<Long, Long> modelIdByOrderId = new HashMap<>();
        for (XianyuOrderDO order : orders) {
            if (!StringUtils.hasText(order.getXianyuItemId())) {
                continue;
            }
            RentalChannelProductRuleDO rule =
                    ruleByShopAndItem.get(shopItemKey(order.getShopId(), order.getXianyuItemId()));
            if (rule == null) {
                continue;
            }
            if ("SINGLE".equals(rule.getMappingMode())) {
                if (rule.getSingleDeviceModelId() != null) {
                    modelIdByOrderId.put(order.getId(), rule.getSingleDeviceModelId());
                }
            } else if ("MULTI".equals(rule.getMappingMode())
                    && StringUtils.hasText(order.getXgjSkuId())) {
                Long modelId =
                        modelIdByRuleAndSku.get(rule.getId() + "|" + order.getXgjSkuId().trim());
                if (modelId != null) {
                    modelIdByOrderId.put(order.getId(), modelId);
                }
            }
        }
        if (modelIdByOrderId.isEmpty()) {
            return Map.of();
        }
        List<Long> modelIds = modelIdByOrderId.values().stream().distinct().toList();
        Map<Long, String> modelCodeById = new HashMap<>();
        for (RentalDeviceModelDO model : modelMapper.selectByIds(modelIds)) {
            if (Boolean.TRUE.equals(model.getEnabled()) && StringUtils.hasText(model.getModelCode())) {
                modelCodeById.put(model.getId(), model.getModelCode().trim());
            }
        }
        Map<Long, String> modelCodeByOrderId = new HashMap<>();
        modelIdByOrderId.forEach((orderId, modelId) -> {
            String modelCode = modelCodeById.get(modelId);
            if (modelCode != null) {
                modelCodeByOrderId.put(orderId, modelCode);
            }
        });
        return modelCodeByOrderId;
    }

    private static String shopItemKey(Long shopId, String xianyuItemId) {
        return shopId + "|" + xianyuItemId.trim();
    }

    private void enrichExactXianyuSku(XianyuOrderDO source) {
        if (StringUtils.hasText(source.getXianyuSkuId())
                || !StringUtils.hasText(source.getXianyuItemId())
                || !StringUtils.hasText(source.getXgjSkuId())) {
            return;
        }
        XianyuProductDO product = productMapper.selectByShopIdAndXianyuItemId(
                source.getShopId(), source.getXianyuItemId().trim());
        if (product == null || !StringUtils.hasText(source.getXgjProductId())
                || !StringUtils.hasText(product.getXgjProductId())
                || !source.getXgjProductId().trim().equals(product.getXgjProductId().trim())) {
            return;
        }
        XianyuProductSkuDO sku = productSkuMapper.selectByProductIdAndXgjSkuIdForUpdate(
                product.getId(), source.getXgjSkuId().trim());
        if (sku != null && StringUtils.hasText(sku.getXianyuSkuId())) {
            source.setXianyuSkuId(sku.getXianyuSkuId().trim());
        }
    }

    private void applyPreparation(XianyuOrderDO source,
                                  RentalOrderDO rentalOrder,
                                  RentalOrderPreparationDecision preparation,
                                  LocalDateTime updatedAt) {
        source.setPreparationStatus(preparation.status());
        source.setPreparationReasonCode(preparation.reasonCode());
        source.setPreparationUpdatedAt(updatedAt);
        rentalOrder.setPreparationStatus(preparation.status());
        rentalOrder.setPreparationReasonCode(preparation.reasonCode());
        rentalOrder.setPreparationUpdatedAt(updatedAt);
    }

    private void markConfigurationSkipped(XianyuOrderDO source) {
        LocalDateTime updatedAt = LocalDateTime.now(BUSINESS_ZONE);
        source.setPreparationStatus(HANDLING_POLICY_SKIPPED);
        source.setPreparationReasonCode(null);
        source.setPreparationUpdatedAt(updatedAt);
        source.setConversionStatus(HANDLING_POLICY_SKIPPED);
        source.setRentalOrderId(null);
        source.setUpdater(SYSTEM_OPERATOR);
        xianyuOrderMapper.updateById(source);
    }

    private void markIneligible(XianyuOrderDO source, String reasonCode) {
        LocalDateTime updatedAt = LocalDateTime.now(BUSINESS_ZONE);
        source.setPreparationStatus("INELIGIBLE");
        source.setPreparationReasonCode(reasonCode);
        source.setPreparationUpdatedAt(updatedAt);
        source.setConversionStatus("INELIGIBLE");
        source.setRentalOrderId(null);
        source.setUpdater(SYSTEM_OPERATOR);
        xianyuOrderMapper.updateById(source);
    }

    private RentalChannelOrderReconciliationResult requireReview(XianyuOrderDO source, String reasonCode) {
        return openReview(source, REVIEW_TYPE, reasonCode,
                "Channel order reconciliation requires operator review");
    }

    private RentalChannelOrderReconciliationResult requireFulfillmentReview(
            XianyuOrderDO source, String reasonCode) {
        return openReview(source, FULFILLMENT_REVIEW_TYPE, reasonCode,
                "Seller remark update requires fulfillment review");
    }

    private RentalChannelOrderReconciliationResult openReview(
            XianyuOrderDO source, String reviewType, String reasonCode, String reasonMessage) {
        LocalDateTime updatedAt = LocalDateTime.now(BUSINESS_ZONE);
        source.setPreparationStatus(CONVERSION_STATUS_REVIEW_REQUIRED);
        source.setPreparationReasonCode(reasonCode);
        source.setPreparationUpdatedAt(updatedAt);
        source.setConversionStatus(CONVERSION_STATUS_REVIEW_REQUIRED);
        source.setUpdater(SYSTEM_OPERATOR);
        xianyuOrderMapper.updateById(source);
        RentalManualReviewDO review = manualReviewMapper.selectBySourceAndReviewTypeForUpdate(
                REVIEW_SOURCE_TYPE, source.getId().toString(), reviewType);
        if (review == null) {
            review = RentalManualReviewDO.builder()
                    .reviewType(reviewType)
                    .sourceType(REVIEW_SOURCE_TYPE)
                    .sourceIdentifier(source.getId().toString())
                    .status(OPEN.getStatus())
                    .reasonCode(reasonCode)
                    .reasonMessage(reasonMessage)
                    .build();
            review.setTenantId(source.getTenantId());
            review.setCreator(SYSTEM_OPERATOR);
            review.setUpdater(SYSTEM_OPERATOR);
            manualReviewMapper.insert(review);
        } else if (!OPEN.getStatus().equals(review.getStatus())
                || !Objects.equals(reasonCode, review.getReasonCode())) {
            review.setStatus(OPEN.getStatus());
            review.setReasonCode(reasonCode);
            review.setReasonMessage(reasonMessage);
            review.setResolutionNote(null);
            review.setResolvedBy(null);
            review.setResolvedAt(null);
            review.setUpdater(SYSTEM_OPERATOR);
            manualReviewMapper.updateById(review);
        }
        return RentalChannelOrderReconciliationResult.reviewRequired(review.getId(), reasonCode);
    }

    private void resolveConversionReview(Long channelOrderId, String resolutionNote) {
        RentalManualReviewDO review = manualReviewMapper.selectBySourceAndReviewTypeForUpdate(
                REVIEW_SOURCE_TYPE, channelOrderId.toString(), REVIEW_TYPE);
        if (review == null || !OPEN.getStatus().equals(review.getStatus())) {
            return;
        }
        review.setStatus(RESOLVED.getStatus());
        review.setResolutionNote(resolutionNote);
        review.setResolvedAt(LocalDateTime.now(BUSINESS_ZONE));
        review.setUpdater(SYSTEM_OPERATOR);
        manualReviewMapper.updateById(review);
    }

    private static boolean isSkipped(RentalChannelProductRuleDO rule) {
        return rule != null && Boolean.TRUE.equals(rule.getEnabled())
                && HANDLING_POLICY_SKIPPED.equals(rule.getHandlingPolicy());
    }

    private static String trimToNull(String value) {
        return !StringUtils.hasText(value) ? null : value.trim();
    }

    private static String sourceIdentity(XianyuOrderDO source) {
        return source.getShopId() + ":" + source.getExternalOrderId();
    }

    private record SourceOrderSemanticState(
            Long sourceRentalOrderId,
            String sourceXianyuSkuId,
            String sourceConversionStatus,
            String sourcePreparationStatus,
            String sourcePreparationReasonCode,
            LocalDate orderBillableStartDate,
            LocalDate orderBillableEndDate,
            LocalDate orderOccupyStartDate,
            LocalDate orderOccupyEndDateExclusive,
            LocalDate orderExpectedSendBackDate,
            String orderPreparationStatus,
            String orderPreparationReasonCode,
            String orderConversionVersion) {

        static SourceOrderSemanticState capture(
                XianyuOrderDO source, RentalOrderDO order) {
            return new SourceOrderSemanticState(
                    source.getRentalOrderId(),
                    source.getXianyuSkuId(),
                    source.getConversionStatus(),
                    source.getPreparationStatus(),
                    source.getPreparationReasonCode(),
                    order.getBillableStartDate(),
                    order.getBillableEndDate(),
                    order.getOccupyStartDate(),
                    order.getOccupyEndDateExclusive(),
                    order.getExpectedSendBackDate(),
                    order.getPreparationStatus(),
                    order.getPreparationReasonCode(),
                    order.getConversionVersion());
        }

        boolean differsFrom(XianyuOrderDO source, RentalOrderDO order) {
            return !Objects.equals(sourceRentalOrderId, source.getRentalOrderId())
                    || !Objects.equals(sourceXianyuSkuId, source.getXianyuSkuId())
                    || !Objects.equals(sourceConversionStatus, source.getConversionStatus())
                    || !Objects.equals(sourcePreparationStatus, source.getPreparationStatus())
                    || !Objects.equals(sourcePreparationReasonCode, source.getPreparationReasonCode())
                    || !Objects.equals(orderBillableStartDate, order.getBillableStartDate())
                    || !Objects.equals(orderBillableEndDate, order.getBillableEndDate())
                    || !Objects.equals(orderOccupyStartDate, order.getOccupyStartDate())
                    || !Objects.equals(orderOccupyEndDateExclusive, order.getOccupyEndDateExclusive())
                    || !Objects.equals(orderExpectedSendBackDate, order.getExpectedSendBackDate())
                    || !Objects.equals(orderPreparationStatus, order.getPreparationStatus())
                    || !Objects.equals(orderPreparationReasonCode, order.getPreparationReasonCode())
                    || !Objects.equals(orderConversionVersion, order.getConversionVersion());
        }
    }

    private record ItemSemanticState(
            String equipmentModelCode,
            String sourceProductId,
            String sourceSkuId,
            LocalDate billableStartDate,
            LocalDate billableEndDate,
            LocalDate occupyStartDate,
            LocalDate occupyEndDateExclusive,
            LocalDate expectedSendBackDate) {

        static ItemSemanticState capture(RentalOrderItemDO item) {
            return new ItemSemanticState(
                    item.getEquipmentModelCode(),
                    item.getSourceProductId(),
                    item.getSourceSkuId(),
                    item.getBillableStartDate(),
                    item.getBillableEndDate(),
                    item.getOccupyStartDate(),
                    item.getOccupyEndDateExclusive(),
                    item.getExpectedSendBackDate());
        }

        boolean differsFrom(RentalOrderItemDO item) {
            return !Objects.equals(equipmentModelCode, item.getEquipmentModelCode())
                    || !Objects.equals(sourceProductId, item.getSourceProductId())
                    || !Objects.equals(sourceSkuId, item.getSourceSkuId())
                    || !Objects.equals(billableStartDate, item.getBillableStartDate())
                    || !Objects.equals(billableEndDate, item.getBillableEndDate())
                    || !Objects.equals(occupyStartDate, item.getOccupyStartDate())
                    || !Objects.equals(occupyEndDateExclusive, item.getOccupyEndDateExclusive())
                    || !Objects.equals(expectedSendBackDate, item.getExpectedSendBackDate());
        }
    }

    private record ModelResolution(String modelCode, String reasonCode) {

        private static ModelResolution missing(String reasonCode) {
            return new ModelResolution(null, reasonCode);
        }

    }

}
