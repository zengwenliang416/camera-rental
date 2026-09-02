package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationTrigger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class XianyuProductPersistenceService {

    static final String PRODUCT_DETAIL_SOURCE_TYPE = "PRODUCT_DETAIL";
    static final String PRODUCT_DETAIL_SCHEMA_VERSION = "XIAN_GUAN_JIA_PRODUCT_DETAIL_V2";
    static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";

    private final XianyuProductDetailPayloadParser payloadParser;
    private final XianyuPayloadHasher payloadHasher;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuProductMapper productMapper;
    private final XianyuShopMapper shopMapper;
    private final RentalChannelOrderReconciliationTrigger reconciliationTrigger;
    private final Clock clock;

    public XianyuProductPersistenceService(XianyuProductDetailPayloadParser payloadParser,
                                           XianyuPayloadHasher payloadHasher,
                                           XianyuRawPayloadMapper rawPayloadMapper,
                                           XianyuProductMapper productMapper,
                                           XianyuShopMapper shopMapper,
                                           RentalChannelOrderReconciliationTrigger reconciliationTrigger,
                                           @Qualifier("xianyuClock") Clock clock) {
        this.payloadParser = payloadParser;
        this.payloadHasher = payloadHasher;
        this.rawPayloadMapper = rawPayloadMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.reconciliationTrigger = reconciliationTrigger;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public XianyuProductDO persistProductDetail(Long shopId, String rawPayload) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(rawPayload, "rawPayload");
        XianyuProductSnapshot snapshot = payloadParser.parse(rawPayload);
        XianyuShopDO shop = requireShop(shopId);
        String xianyuItemId = resolveOwnedItemId(snapshot, shop);
        Long rawPayloadId = persistRawPayload(shopId, snapshot.xgjProductId(), rawPayload);
        XianyuProductDO existing = productMapper.selectByShopIdAndXgjProductIdForUpdate(
                shopId, snapshot.xgjProductId());
        if (isOlderThanStoredSnapshot(existing, snapshot)) {
            return existing;
        }
        XianyuProductDO product = XianyuProductDO.builder()
                .id(existing == null ? null : existing.getId())
                .shopId(shopId)
                .xgjProductId(snapshot.xgjProductId())
                .xianyuItemId(xianyuItemId)
                .title(snapshot.title())
                .categoryId(snapshot.categoryId())
                .status(snapshot.status())
                .sourceUpdatedAt(snapshot.sourceUpdatedAt())
                .rawPayloadId(rawPayloadId)
                .build();
        if (existing == null) {
            product.setCreator("system");
            product.setUpdater("system");
            productMapper.insert(product);
        } else {
            product.setUpdater("system");
            productMapper.updateById(product);
        }
        reconciliationTrigger.afterProductChange(shopId, product.getXgjProductId());
        return product;
    }

    private XianyuShopDO requireShop(Long shopId) {
        XianyuShopDO shop = shopMapper.selectByTenantIdAndId(TenantContextHolder.getRequiredTenantId(), shopId);
        if (shop == null || shop.getXianyuUserName() == null || shop.getXianyuUserName().isBlank()) {
            throw new IllegalStateException("Shop is missing its synchronized Xianyu user name");
        }
        return shop;
    }

    /**
     * 商品详情里的 publish_shop 只保留发布到本店铺账号的条目。
     * 闲管家商品是应用级数据，同一商品可能只发布到其他闲鱼账号；没有本店条目时
     * 返回 null 并照常落库（维持去重水位，避免每次同步重复拉详情），但商品不会
     * 关联闲鱼商品 ID，也不会被该店铺的渠道商品规则匹配。同一账号出现多条发布
     * 条目属于歧义，仍然拒绝落库。
     */
    private String resolveOwnedItemId(XianyuProductSnapshot snapshot, XianyuShopDO shop) {
        List<XianyuPublishedItem> ownedItems = snapshot.publishedItems().stream()
                .filter(item -> shop.getXianyuUserName().equals(item.xianyuUserName()))
                .toList();
        if (ownedItems.isEmpty()) {
            return null;
        }
        if (ownedItems.size() != 1) {
            throw new IllegalStateException("Product detail does not contain exactly one item for the synchronized shop");
        }
        return ownedItems.get(0).xianyuItemId();
    }

    private boolean isOlderThanStoredSnapshot(XianyuProductDO existing, XianyuProductSnapshot snapshot) {
        if (existing == null || existing.getSourceUpdatedAt() == null) {
            return false;
        }
        return snapshot.sourceUpdatedAt() == null
                || snapshot.sourceUpdatedAt().isBefore(existing.getSourceUpdatedAt());
    }

    private Long persistRawPayload(Long shopId, String externalProductId, String rawPayload) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String sourceIdentifier = "product:" + shopId + ":" + externalProductId;
        String payloadHash = payloadHasher.sha256(rawPayload);
        XianyuRawPayloadDO payload = XianyuRawPayloadDO.builder()
                .sourceType(PRODUCT_DETAIL_SOURCE_TYPE)
                .sourceIdentifier(sourceIdentifier)
                .payloadHash(payloadHash)
                .schemaVersion(PRODUCT_DETAIL_SCHEMA_VERSION)
                .redactionVersion(RESTRICTED_PAYLOAD_POLICY)
                .payload(rawPayload)
                .receivedAt(LocalDateTime.now(clock))
                .build();
        payload.setCreator("system");
        payload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, payload);
        XianyuRawPayloadDO existing = rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(
                tenantId, PRODUCT_DETAIL_SOURCE_TYPE, sourceIdentifier, payloadHash);
        if (existing == null) {
            throw new IllegalStateException("Product detail payload disappeared after insert");
        }
        return existing.getId();
    }

}
