package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncCursorMapper;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriodParser;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
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
 * After a successful order-detail upsert, triggers Hermes-style automatic conversion.
 */
@Service
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
    private final XianyuRentalConversionService conversionService;
    private final SellerRemarkRentalPeriodParser rentalPeriodParser;
    private final Clock clock;

    public XianyuOrderPersistenceServiceImpl(XianyuOrderPayloadParser payloadParser,
                                             XianyuPayloadHasher payloadHasher,
                                             XianyuRawPayloadMapper rawPayloadMapper,
                                             XianyuOrderMapper orderMapper,
                                             XianyuSyncCursorMapper cursorMapper,
                                             XianyuSyncCursorAdvancer cursorAdvancer,
                                             XianyuRentalConversionService conversionService,
                                             SellerRemarkRentalPeriodParser rentalPeriodParser,
                                             @Qualifier("xianyuClock") Clock clock) {
        this.payloadParser = payloadParser;
        this.payloadHasher = payloadHasher;
        this.rawPayloadMapper = rawPayloadMapper;
        this.orderMapper = orderMapper;
        this.cursorMapper = cursorMapper;
        this.cursorAdvancer = cursorAdvancer;
        this.conversionService = conversionService;
        this.rentalPeriodParser = rentalPeriodParser;
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
        if (isOlderThanStoredSnapshot(existing, snapshot)) {
            return existing;
        }
        SellerRemarkRentalPeriod rentalPeriod = rentalPeriodParser.parse(
                snapshot.sellerRemark(), referenceDate(snapshot.orderTime(), snapshot.sourceCreatedAt()));
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(existing == null ? null : existing.getId())
                .shopId(shopId)
                .externalOrderId(snapshot.externalOrderId())
                .externalProductId(snapshot.externalProductId())
                .externalSkuId(snapshot.externalSkuId())
                .orderStatus(snapshot.orderStatus())
                .payAmount(snapshot.payAmount())
                .currency("CNY")
                .sellerRemark(snapshot.sellerRemark())
                .remarkParseVersion(rentalPeriod.version())
                .remarkParseStatus(rentalPeriod.status())
                .billableStartDate(rentalPeriod.billableStartDate())
                .billableEndDate(rentalPeriod.billableEndDate())
                .shipDate(rentalPeriod.shipDate())
                .receiveDate(rentalPeriod.receiveDate())
                .returnDate(rentalPeriod.returnDate())
                .rentalPeriodStatus(rentalPeriod.status())
                .rentalPeriodReasonCode(rentalPeriod.reasonCode())
                .sourceCreatedAt(snapshot.sourceCreatedAt())
                .sourceUpdatedAt(snapshot.sourceUpdatedAt())
                .rawPayloadId(rawPayloadId)
                .conversionStatus(existing == null || existing.getConversionStatus() == null
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
        // Hermes-style: durable channel fact → automatic remark parse / convert / review.
        conversionService.autoConvertAfterPersist(order.getId());
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
                SellerRemarkRentalPeriodParser.VERSION, boundedLimit);
        for (XianyuOrderDO order : orders) {
            SellerRemarkRentalPeriod rentalPeriod = rentalPeriodParser.parse(
                    order.getSellerRemark(), referenceDate(order.getOrderTime(), order.getSourceCreatedAt()));
            applyRentalPeriod(order, rentalPeriod);
            order.setUpdater("system");
            orderMapper.updateById(order);
        }
        return orders.size();
    }

    private static void applyRentalPeriod(XianyuOrderDO order, SellerRemarkRentalPeriod rentalPeriod) {
        order.setRemarkParseVersion(rentalPeriod.version());
        order.setRemarkParseStatus(rentalPeriod.status());
        order.setBillableStartDate(rentalPeriod.billableStartDate());
        order.setBillableEndDate(rentalPeriod.billableEndDate());
        order.setShipDate(rentalPeriod.shipDate());
        order.setReceiveDate(rentalPeriod.receiveDate());
        order.setReturnDate(rentalPeriod.returnDate());
        order.setRentalPeriodStatus(rentalPeriod.status());
        order.setRentalPeriodReasonCode(rentalPeriod.reasonCode());
    }

    private static LocalDate referenceDate(LocalDateTime orderTime, LocalDateTime sourceCreatedAt) {
        if (orderTime != null) {
            return orderTime.toLocalDate();
        }
        return sourceCreatedAt == null ? null : sourceCreatedAt.toLocalDate();
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
