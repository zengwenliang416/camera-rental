package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderRemarkHistoryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncCursorMapper;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriodResolver;
import cn.iocoder.yudao.module.rental.service.SellerRemarkResolution;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import cn.iocoder.yudao.module.rental.service.logistics.XianyuOrderDeliverySyncService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationResult;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalRemarkPlanChangeClassifier;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalRemarkPlanChangeType;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalRemarkPlanUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Persists channel evidence before normalized order facts and never performs transport or logging.
 * After a successful order-detail upsert, triggers authoritative rental reconciliation.
 */
@Service
@Slf4j
public class XianyuOrderPersistenceServiceImpl implements XianyuOrderPersistenceService {

    static final String ORDER_DETAIL_SOURCE_TYPE = "ORDER_DETAIL";
    static final String ORDER_CURSOR_RESOURCE_TYPE = "ORDER";
    static final String ORDER_DETAIL_SCHEMA_VERSION = "XIAN_GUAN_JIA_ORDER_DETAIL_V1";
    static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";

    private final XianyuOrderPayloadParser payloadParser;
    private final XianyuPayloadHasher payloadHasher;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuOrderMapper orderMapper;
    private final XianyuSyncCursorMapper cursorMapper;
    private final XianyuSyncCursorAdvancer cursorAdvancer;
    private final RentalChannelOrderReconciliationService reconciliationService;
    private final XianyuOrderDeliverySyncService deliverySyncService;
    private final SellerRemarkRentalPeriodResolver rentalPeriodResolver;
    private final XianyuOrderRemarkHistoryService remarkHistoryService;
    private final RentalRemarkPlanChangeClassifier remarkChangeClassifier;
    private final Clock clock;

    public XianyuOrderPersistenceServiceImpl(XianyuOrderPayloadParser payloadParser,
                                             XianyuPayloadHasher payloadHasher,
                                             XianyuRawPayloadMapper rawPayloadMapper,
                                             XianyuOrderMapper orderMapper,
                                             XianyuSyncCursorMapper cursorMapper,
                                             XianyuSyncCursorAdvancer cursorAdvancer,
                                             RentalChannelOrderReconciliationService reconciliationService,
                                             XianyuOrderDeliverySyncService deliverySyncService,
                                             SellerRemarkRentalPeriodResolver rentalPeriodResolver,
                                             XianyuOrderRemarkHistoryService remarkHistoryService,
                                             RentalRemarkPlanChangeClassifier remarkChangeClassifier,
                                             @Qualifier("xianyuClock") Clock clock) {
        this.payloadParser = payloadParser;
        this.payloadHasher = payloadHasher;
        this.rawPayloadMapper = rawPayloadMapper;
        this.orderMapper = orderMapper;
        this.cursorMapper = cursorMapper;
        this.cursorAdvancer = cursorAdvancer;
        this.reconciliationService = reconciliationService;
        this.deliverySyncService = deliverySyncService;
        this.rentalPeriodResolver = rentalPeriodResolver;
        this.remarkHistoryService = remarkHistoryService;
        this.remarkChangeClassifier = remarkChangeClassifier;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XianyuOrderDO persistOrderDetail(Long shopId, String rawPayload) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(rawPayload, "rawPayload");
        XianyuOrderSnapshot snapshot = payloadParser.parse(rawPayload);
        Long rawPayloadId = persistRawPayload(shopId, snapshot.externalOrderId(), rawPayload);
        XianyuOrderDO existing = orderMapper.selectByShopIdAndExternalOrderIdForUpdate(
                shopId, snapshot.externalOrderId());
        boolean configurationSkipped = reconciliationService.isConfigurationSkipped(
                shopId, snapshot.xianyuItemId(), existing != null && existing.getRentalOrderId() != null);
        SellerRemarkResolution resolution = configurationSkipped ? null : rentalPeriodResolver.resolve(
                snapshot.sellerRemark(), referenceDate(snapshot.orderTime(), snapshot.sourceCreatedAt()));
        SellerRemarkRentalPeriod rentalPeriod = resolution == null ? null : resolution.period();
        SellerRemarkRentalPeriod previousEffectivePlan = effectivePlan(existing);
        RentalRemarkPlanChangeType changeType = configurationSkipped ? null
                : remarkChangeClassifier.classify(
                        snapshot.sellerRemark(), previousEffectivePlan, rentalPeriod);
        boolean effectiveCandidate = !configurationSkipped && isEffectiveCandidate(rentalPeriod, changeType);
        if (isOlderThanStoredSnapshot(existing, snapshot)) {
            if (!configurationSkipped) {
                remarkHistoryService.record(existing.getId(), rawPayloadId, snapshot.sellerRemark(),
                        snapshot.sourceUpdatedAt(), rentalPeriod, false, changeType);
            }
            reconciliationService.reconcile(existing.getId());
            return existing;
        }
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(existing == null ? null : existing.getId())
                .shopId(shopId)
                .externalOrderId(snapshot.externalOrderId())
                .xgjProductId(snapshot.xgjProductId())
                .xianyuItemId(snapshot.xianyuItemId())
                .xgjSkuId(snapshot.xgjSkuId())
                .xianyuSkuId(existing == null ? null : existing.getXianyuSkuId())
                .preparationStatus(configurationSkipped ? "CONFIG_SKIPPED"
                        : existing == null || existing.getPreparationStatus() == null
                                ? "WAITING_RECONCILIATION" : existing.getPreparationStatus())
                .preparationReasonCode(configurationSkipped
                        ? null : existing == null ? null : existing.getPreparationReasonCode())
                .preparationUpdatedAt(existing == null ? null : existing.getPreparationUpdatedAt())
                .orderStatus(snapshot.orderStatus())
                .payAmount(snapshot.payAmount())
                .currency("CNY")
                .sellerRemark(snapshot.sellerRemark())
                .remarkParseVersion(configurationSkipped
                        ? value(existing, XianyuOrderDO::getRemarkParseVersion) : rentalPeriod.version())
                .remarkParseStatus(configurationSkipped ? "SKIPPED" : rentalPeriod.status())
                .remarkParseSource(configurationSkipped
                        ? value(existing, XianyuOrderDO::getRemarkParseSource) : resolution.source())
                .remarkParseConfidence(configurationSkipped
                        ? value(existing, XianyuOrderDO::getRemarkParseConfidence) : resolution.confidence())
                .remarkParseModel(configurationSkipped
                        ? value(existing, XianyuOrderDO::getRemarkParseModel) : resolution.model())
                .remarkParseEvidenceJson(configurationSkipped
                        ? value(existing, XianyuOrderDO::getRemarkParseEvidenceJson) : resolution.evidenceJson())
                .billableStartDate(value(existing, XianyuOrderDO::getBillableStartDate))
                .billableEndDate(value(existing, XianyuOrderDO::getBillableEndDate))
                .shipDate(value(existing, XianyuOrderDO::getShipDate))
                .receiveDate(value(existing, XianyuOrderDO::getReceiveDate))
                .returnDate(value(existing, XianyuOrderDO::getReturnDate))
                .rentalPeriodStatus(configurationSkipped ? "SKIPPED" : rentalPeriod.status())
                .rentalPeriodReasonCode(configurationSkipped ? null : rentalPeriod.reasonCode())
                .sourceCreatedAt(snapshot.sourceCreatedAt())
                .sourceUpdatedAt(snapshot.sourceUpdatedAt())
                .rawPayloadId(rawPayloadId)
                .conversionStatus(configurationSkipped ? "CONFIG_SKIPPED"
                        : existing == null || existing.getConversionStatus() == null
                                ? "PENDING" : existing.getConversionStatus())
                .rentalOrderId(existing == null ? null : existing.getRentalOrderId())
                .detailJson(snapshot.detailJson())
                // The API omits receiver fields after shipment, so never replace known values with blanks.
                .receiverName(preferIncoming(
                        snapshot.receiverName(), existing == null ? null : existing.getReceiverName()))
                .receiverMobile(preferIncoming(
                        snapshot.receiverMobile(), existing == null ? null : existing.getReceiverMobile()))
                .receiverAddress(preferIncoming(
                        snapshot.receiverAddress(), existing == null ? null : existing.getReceiverAddress()))
                .orderType(snapshot.orderType())
                .orderTime(snapshot.orderTime())
                .totalAmount(snapshot.totalAmount())
                .payNo(snapshot.payNo())
                .payTime(snapshot.payTime())
                .refundStatus(snapshot.refundStatus())
                .refundAmount(snapshot.refundAmount())
                .refundTime(snapshot.refundTime())
                .waybillNo(snapshot.waybillNo())
                .expressCode(snapshot.expressCode())
                .expressName(snapshot.expressName())
                .expressFee(snapshot.expressFee())
                .consignType(snapshot.consignType())
                .consignTime(snapshot.consignTime())
                .confirmTime(snapshot.confirmTime())
                .cancelReason(snapshot.cancelReason())
                .cancelTime(snapshot.cancelTime())
                .buyerNick(snapshot.buyerNick())
                .sellerName(snapshot.sellerName())
                .goodsTitle(snapshot.goodsTitle())
                .goodsQuantity(snapshot.goodsQuantity())
                .goodsPrice(snapshot.goodsPrice())
                .goodsJson(snapshot.goodsJson())
                .xybSellerAmount(snapshot.xybSellerAmount())
                .taxIncluded(snapshot.taxIncluded())
                .idleBizType(snapshot.idleBizType())
                .pinGroupStatus(snapshot.pinGroupStatus())
                .build();
        // Job context has no login user; DB columns require non-null creator/updater.
        if (existing == null) {
            order.setCreator("system");
            order.setUpdater("system");
            orderMapper.insert(order);
        } else {
            order.setUpdater("system");
            orderMapper.updateById(order);
        }
        XianyuOrderRemarkHistoryDO history = configurationSkipped ? null
                : remarkHistoryService.record(order.getId(), rawPayloadId, snapshot.sellerRemark(),
                        snapshot.sourceUpdatedAt(), rentalPeriod, false, changeType);
        RentalChannelOrderReconciliationResult reconciliation = configurationSkipped
                ? reconciliationService.reconcile(order.getId())
                : reconciliationService.reconcile(
                        order.getId(), new RentalRemarkPlanUpdate(previousEffectivePlan, rentalPeriod, changeType));
        applyEffectivePlanIfAccepted(order, rentalPeriod, effectiveCandidate, reconciliation, history);
        syncDelivery(orderMapper.selectById(order.getId()));
        return order;
    }

    private static String preferIncoming(String incoming, String existing) {
        return incoming == null || incoming.isBlank() ? existing : incoming.trim();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int backfillMissingRentalPeriods(int limit) {
        int boundedLimit = Math.max(1, Math.min(500, limit));
        List<XianyuOrderDO> orders = orderMapper.selectMissingRentalPeriodRefs(
                SellerRemarkRentalPeriodResolver.VERSION, boundedLimit);
        int processed = 0;
        for (XianyuOrderDO candidate : orders) {
            XianyuOrderDO order = orderMapper.selectByIdForUpdate(candidate.getId());
            if (order == null || "CONFIG_SKIPPED".equals(order.getConversionStatus())) {
                continue;
            }
            SellerRemarkRentalPeriod previousEffectivePlan = effectivePlan(order);
            SellerRemarkResolution resolution = rentalPeriodResolver.resolve(
                    order.getSellerRemark(), referenceDate(order.getOrderTime(), order.getSourceCreatedAt()));
            RentalRemarkPlanChangeType changeType = remarkChangeClassifier.classify(
                    order.getSellerRemark(), previousEffectivePlan, resolution.period());
            boolean effectiveCandidate = isEffectiveCandidate(resolution.period(), changeType);
            applyRentalPeriod(order, resolution);
            order.setUpdater("system");
            orderMapper.updateById(order);
            XianyuOrderRemarkHistoryDO history = remarkHistoryService.record(
                    order.getId(), order.getRawPayloadId(), order.getSellerRemark(),
                    order.getSourceUpdatedAt(), resolution.period(), false, changeType);
            RentalChannelOrderReconciliationResult reconciliation =
                    reconciliationService.reconcile(order.getId(),
                            new RentalRemarkPlanUpdate(previousEffectivePlan, resolution.period(), changeType));
            applyEffectivePlanIfAccepted(
                    order, resolution.period(), effectiveCandidate, reconciliation, history);
            syncDelivery(orderMapper.selectById(order.getId()));
            processed++;
        }
        return processed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int reparseRentalPeriods(int limit) {
        int remaining = Math.max(1, Math.min(10_000, limit));
        int processed = 0;
        Long beforeId = null;
        while (remaining > 0) {
            List<XianyuOrderDO> orders = orderMapper.selectRemarkReparseCandidates(
                    beforeId, Math.min(500, remaining));
            if (orders.isEmpty()) {
                break;
            }
            for (XianyuOrderDO candidate : orders) {
                XianyuOrderDO order = orderMapper.selectByIdForUpdate(candidate.getId());
                if (order == null || "CONFIG_SKIPPED".equals(order.getConversionStatus())) {
                    continue;
                }
                SellerRemarkRentalPeriod previousEffectivePlan = effectivePlan(order);
                SellerRemarkResolution resolution = rentalPeriodResolver.resolve(
                        order.getSellerRemark(), referenceDate(order.getOrderTime(), order.getSourceCreatedAt()));
                RentalRemarkPlanChangeType changeType = remarkChangeClassifier.classify(
                        order.getSellerRemark(), previousEffectivePlan, resolution.period());
                boolean effectiveCandidate = isEffectiveCandidate(resolution.period(), changeType);
                applyRentalPeriod(order, resolution);
                order.setUpdater("system");
                orderMapper.updateById(order);
                XianyuOrderRemarkHistoryDO history = remarkHistoryService.record(
                        order.getId(), order.getRawPayloadId(), order.getSellerRemark(),
                        order.getSourceUpdatedAt(), resolution.period(), false, changeType);
                RentalChannelOrderReconciliationResult reconciliation =
                        reconciliationService.reconcile(order.getId(),
                                new RentalRemarkPlanUpdate(previousEffectivePlan, resolution.period(), changeType));
                applyEffectivePlanIfAccepted(
                        order, resolution.period(), effectiveCandidate, reconciliation, history);
                syncDelivery(orderMapper.selectById(order.getId()));
                processed++;
            }
            beforeId = orders.get(orders.size() - 1).getId();
            remaining -= orders.size();
        }
        return processed;
    }

    private void syncDelivery(XianyuOrderDO order) {
        try {
            deliverySyncService.syncOutboundIfTrackable(order);
        } catch (RentalLogisticsException ex) {
            log.warn("[xianyu][delivery-sync] channelOrderId={} skipped: {}",
                    order == null ? null : order.getId(), ex.getCode());
        }
    }

    private static void applyRentalPeriod(XianyuOrderDO order, SellerRemarkResolution resolution) {
        SellerRemarkRentalPeriod rentalPeriod = resolution.period();
        order.setRemarkParseVersion(rentalPeriod.version());
        order.setRemarkParseStatus(rentalPeriod.status());
        order.setRemarkParseSource(resolution.source());
        order.setRemarkParseConfidence(resolution.confidence());
        order.setRemarkParseModel(resolution.model());
        order.setRemarkParseEvidenceJson(resolution.evidenceJson());
        order.setRentalPeriodStatus(rentalPeriod.status());
        order.setRentalPeriodReasonCode(rentalPeriod.reasonCode());
    }

    private static LocalDate referenceDate(LocalDateTime orderTime, LocalDateTime sourceCreatedAt) {
        if (orderTime != null) {
            return orderTime.toLocalDate();
        }
        return sourceCreatedAt == null ? null : sourceCreatedAt.toLocalDate();
    }

    private static <T> T value(XianyuOrderDO order, java.util.function.Function<XianyuOrderDO, T> getter) {
        return order == null ? null : getter.apply(order);
    }

    private static boolean isEffectiveCandidate(SellerRemarkRentalPeriod candidate,
                                                RentalRemarkPlanChangeType changeType) {
        return candidate != null && candidate.isSuccess()
                && changeType != RentalRemarkPlanChangeType.AMBIGUOUS;
    }

    private void applyEffectivePlanIfAccepted(
            XianyuOrderDO order,
            SellerRemarkRentalPeriod candidate,
            boolean effectiveCandidate,
            RentalChannelOrderReconciliationResult reconciliation,
            XianyuOrderRemarkHistoryDO history) {
        if (!effectiveCandidate || !reconciliation.planApplied()) {
            return;
        }
        order.setBillableStartDate(candidate.billableStartDate());
        order.setBillableEndDate(candidate.billableEndDate());
        order.setShipDate(candidate.shipDate());
        order.setReceiveDate(candidate.receiveDate());
        order.setReturnDate(candidate.returnDate());
        orderMapper.updateEffectivePlanById(
                order.getId(),
                candidate.billableStartDate(),
                candidate.billableEndDate(),
                candidate.shipDate(),
                candidate.receiveDate(),
                candidate.returnDate());
        remarkHistoryService.markEffective(history);
    }

    private static SellerRemarkRentalPeriod effectivePlan(XianyuOrderDO order) {
        if (order == null || order.getBillableStartDate() == null || order.getBillableEndDate() == null
                || order.getShipDate() == null || order.getReturnDate() == null) {
            return null;
        }
        return SellerRemarkRentalPeriod.success(
                order.getRemarkParseVersion(),
                order.getBillableStartDate(),
                order.getBillableEndDate(),
                order.getShipDate(),
                order.getReceiveDate(),
                order.getReturnDate());
    }

    private boolean isOlderThanStoredSnapshot(XianyuOrderDO existing, XianyuOrderSnapshot snapshot) {
        if (existing == null || existing.getSourceUpdatedAt() == null) {
            return false;
        }
        return snapshot.sourceUpdatedAt() == null
                || snapshot.sourceUpdatedAt().isBefore(existing.getSourceUpdatedAt());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean advanceOrderCursor(Long shopId, LocalDateTime sourceUpdatedAt, String externalOrderId,
                                      LocalDateTime safeUpperBound) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt");
        Objects.requireNonNull(externalOrderId, "externalOrderId");
        Objects.requireNonNull(safeUpperBound, "safeUpperBound");
        XianyuSyncCursorDO current = cursorMapper.selectByShopIdAndResourceTypeForUpdate(
                shopId, ORDER_CURSOR_RESOURCE_TYPE);
        if (!cursorAdvancer.isStrictlyNewer(current, sourceUpdatedAt, externalOrderId)) {
            return false;
        }
        XianyuSyncCursorDO cursor = XianyuSyncCursorDO.builder()
                .id(current == null ? null : current.getId())
                .shopId(shopId)
                .resourceType(ORDER_CURSOR_RESOURCE_TYPE)
                .cursorUpdatedAt(sourceUpdatedAt)
                .cursorExternalId(externalOrderId)
                .safeUpperBound(safeUpperBound)
                .build();
        if (current == null) {
            cursor.setCreator("system");
            cursor.setUpdater("system");
            cursorMapper.insert(cursor);
        } else {
            cursor.setUpdater("system");
            cursorMapper.updateById(cursor);
        }
        return true;
    }

    private Long persistRawPayload(Long shopId, String externalOrderId, String rawPayload) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String sourceIdentifier = "order:" + shopId + ":" + externalOrderId;
        String payloadHash = payloadHasher.sha256(rawPayload);
        XianyuRawPayloadDO payload = XianyuRawPayloadDO.builder()
                .sourceType(ORDER_DETAIL_SOURCE_TYPE)
                .sourceIdentifier(sourceIdentifier)
                .payloadHash(payloadHash)
                .schemaVersion(ORDER_DETAIL_SCHEMA_VERSION)
                .redactionVersion(RESTRICTED_PAYLOAD_POLICY)
                .payload(rawPayload)
                .receivedAt(LocalDateTime.now(clock))
                .build();
        payload.setCreator("system");
        payload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, payload);
        XianyuRawPayloadDO existing = rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(
                tenantId, ORDER_DETAIL_SOURCE_TYPE, sourceIdentifier, payloadHash);
        if (existing == null) {
            throw new IllegalStateException("Order detail payload disappeared after insert");
        }
        return existing.getId();
    }

}
