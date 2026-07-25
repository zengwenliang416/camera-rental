package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncCursorMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Persists channel evidence before normalized order facts and never performs transport or logging.
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
    private final Clock clock;

    public XianyuOrderPersistenceServiceImpl(XianyuOrderPayloadParser payloadParser,
                                             XianyuPayloadHasher payloadHasher,
                                             XianyuRawPayloadMapper rawPayloadMapper,
                                             XianyuOrderMapper orderMapper,
                                             XianyuSyncCursorMapper cursorMapper,
                                             XianyuSyncCursorAdvancer cursorAdvancer,
                                             @Qualifier("xianyuClock") Clock clock) {
        this.payloadParser = payloadParser;
        this.payloadHasher = payloadHasher;
        this.rawPayloadMapper = rawPayloadMapper;
        this.orderMapper = orderMapper;
        this.cursorMapper = cursorMapper;
        this.cursorAdvancer = cursorAdvancer;
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
        boolean remarkChanged = existing == null || !Objects.equals(existing.getSellerRemark(), snapshot.sellerRemark());
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
                .remarkParseVersion(remarkChanged ? null : existing.getRemarkParseVersion())
                .remarkParseStatus(remarkChanged || existing.getRemarkParseStatus() == null
                        ? "PENDING" : existing.getRemarkParseStatus())
                .sourceCreatedAt(snapshot.sourceCreatedAt())
                .sourceUpdatedAt(snapshot.sourceUpdatedAt())
                .rawPayloadId(rawPayloadId)
                .conversionStatus(existing == null || existing.getConversionStatus() == null
                        ? "PENDING" : existing.getConversionStatus())
                .rentalOrderId(existing == null ? null : existing.getRentalOrderId())
                .detailJson(snapshot.detailJson())
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
        return order;
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
