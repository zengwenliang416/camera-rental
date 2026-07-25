package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductSkuMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class XianyuProductSkuPersistenceService {

    static final String PRODUCT_SKU_SOURCE_TYPE = "PRODUCT_SKUS";
    static final String PRODUCT_SKU_SCHEMA_VERSION = "XIAN_GUAN_JIA_PRODUCT_SKUS_V1";
    static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";

    private final XianyuProductSkuPayloadParser payloadParser;
    private final XianyuPayloadHasher payloadHasher;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuProductMapper productMapper;
    private final XianyuProductSkuMapper productSkuMapper;
    private final Clock clock;

    public XianyuProductSkuPersistenceService(XianyuProductSkuPayloadParser payloadParser,
                                              XianyuPayloadHasher payloadHasher,
                                              XianyuRawPayloadMapper rawPayloadMapper,
                                              XianyuProductMapper productMapper,
                                              XianyuProductSkuMapper productSkuMapper,
                                              @Qualifier("xianyuClock") Clock clock) {
        this.payloadParser = payloadParser;
        this.payloadHasher = payloadHasher;
        this.rawPayloadMapper = rawPayloadMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public int persistProductSkus(Long shopId, String rawPayload) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(rawPayload, "rawPayload");
        List<XianyuProductSkuGroup> groups = payloadParser.parse(rawPayload);
        Long rawPayloadId = persistRawPayload(shopId, rawPayload);
        int persisted = 0;
        LocalDateTime now = LocalDateTime.now(clock);
        for (XianyuProductSkuGroup group : groups) {
            XianyuProductDO product = productMapper.selectByShopIdAndExternalProductId(
                    shopId, group.externalProductId());
            if (product == null) {
                throw new IllegalStateException("Product SKU payload references an unknown product");
            }
            for (XianyuProductSkuSnapshot snapshot : group.skuItems()) {
                upsertSku(product.getId(), snapshot, rawPayloadId, now);
                persisted++;
            }
        }
        return persisted;
    }

    private void upsertSku(Long productId, XianyuProductSkuSnapshot snapshot, Long rawPayloadId,
                           LocalDateTime sourceUpdatedAt) {
        XianyuProductSkuDO existing = productSkuMapper.selectByProductIdAndExternalSkuIdForUpdate(
                productId, snapshot.externalSkuId());
        XianyuProductSkuDO sku = XianyuProductSkuDO.builder()
                .id(existing == null ? null : existing.getId())
                .productId(productId)
                .externalSkuId(snapshot.externalSkuId())
                .skuName(snapshot.skuName())
                .sourceStock(snapshot.stock())
                .status("ACTIVE")
                .sourceUpdatedAt(sourceUpdatedAt)
                .rawPayloadId(rawPayloadId)
                .build();
        if (existing == null) {
            sku.setCreator("system");
            sku.setUpdater("system");
            productSkuMapper.insert(sku);
        } else {
            sku.setUpdater("system");
            productSkuMapper.updateById(sku);
        }
    }

    private Long persistRawPayload(Long shopId, String rawPayload) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String payloadHash = payloadHasher.sha256(rawPayload);
        String sourceIdentifier = "product-skus:" + shopId + ":" + payloadHash.substring(0, 16);
        XianyuRawPayloadDO payload = XianyuRawPayloadDO.builder()
                .sourceType(PRODUCT_SKU_SOURCE_TYPE)
                .sourceIdentifier(sourceIdentifier)
                .payloadHash(payloadHash)
                .schemaVersion(PRODUCT_SKU_SCHEMA_VERSION)
                .redactionVersion(RESTRICTED_PAYLOAD_POLICY)
                .payload(rawPayload)
                .receivedAt(LocalDateTime.now(clock))
                .build();
        payload.setCreator("system");
        payload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, payload);
        XianyuRawPayloadDO existing = rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(
                tenantId, PRODUCT_SKU_SOURCE_TYPE, sourceIdentifier, payloadHash);
        if (existing == null) {
            throw new IllegalStateException("Product SKU payload disappeared after insert");
        }
        return existing.getId();
    }

}
