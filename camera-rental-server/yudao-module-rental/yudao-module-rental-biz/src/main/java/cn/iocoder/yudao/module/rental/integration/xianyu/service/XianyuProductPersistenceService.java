package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class XianyuProductPersistenceService {

    static final String PRODUCT_DETAIL_SOURCE_TYPE = "PRODUCT_DETAIL";
    static final String PRODUCT_DETAIL_SCHEMA_VERSION = "XIAN_GUAN_JIA_PRODUCT_DETAIL_V1";
    static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";

    private final XianyuProductDetailPayloadParser payloadParser;
    private final XianyuPayloadHasher payloadHasher;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuProductMapper productMapper;
    private final Clock clock;

    public XianyuProductPersistenceService(XianyuProductDetailPayloadParser payloadParser,
                                           XianyuPayloadHasher payloadHasher,
                                           XianyuRawPayloadMapper rawPayloadMapper,
                                           XianyuProductMapper productMapper,
                                           @Qualifier("xianyuClock") Clock clock) {
        this.payloadParser = payloadParser;
        this.payloadHasher = payloadHasher;
        this.rawPayloadMapper = rawPayloadMapper;
        this.productMapper = productMapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public XianyuProductDO persistProductDetail(Long shopId, String rawPayload) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(rawPayload, "rawPayload");
        XianyuProductSnapshot snapshot = payloadParser.parse(rawPayload);
        Long rawPayloadId = persistRawPayload(shopId, snapshot.externalProductId(), rawPayload);
        XianyuProductDO existing = productMapper.selectByShopIdAndExternalProductIdForUpdate(
                shopId, snapshot.externalProductId());
        if (isOlderThanStoredSnapshot(existing, snapshot)) {
            return existing;
        }
        XianyuProductDO product = XianyuProductDO.builder()
                .id(existing == null ? null : existing.getId())
                .shopId(shopId)
                .externalProductId(snapshot.externalProductId())
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
        return product;
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
