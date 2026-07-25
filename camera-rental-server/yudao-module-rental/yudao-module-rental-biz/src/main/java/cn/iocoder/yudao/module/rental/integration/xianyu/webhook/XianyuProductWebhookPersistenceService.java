package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuLogRedactor;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuPayloadHasher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class XianyuProductWebhookPersistenceService {

    public static final String EVENT_TYPE = "PRODUCT_PUSH";
    private static final String RAW_SOURCE_TYPE = "PRODUCT_PUSH";
    private static final String RAW_SCHEMA_VERSION = "XIAN_GUAN_JIA_PRODUCT_PUSH_V1";
    private static final String RAW_REDACTION_VERSION = "REDACTED_V1";

    private final XianyuPushEventMapper eventMapper;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuPayloadHasher payloadHasher;
    private final XianyuLogRedactor logRedactor;
    private final XianyuPushEventPublisher eventPublisher;
    private final Clock clock;

    public XianyuProductWebhookPersistenceService(XianyuPushEventMapper eventMapper,
                                                  XianyuRawPayloadMapper rawPayloadMapper,
                                                  XianyuPayloadHasher payloadHasher,
                                                  XianyuLogRedactor logRedactor,
                                                  XianyuPushEventPublisher eventPublisher,
                                                  @Qualifier("xianyuClock") Clock clock) {
        this.eventMapper = eventMapper;
        this.rawPayloadMapper = rawPayloadMapper;
        this.payloadHasher = payloadHasher;
        this.logRedactor = logRedactor;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public String dedupeKey(XianyuProductPushPayload payload) {
        return payloadHasher.sha256(String.join("|",
                EVENT_TYPE,
                payload.sellerId(),
                payload.externalProductId(),
                Long.toString(payload.modifyTime()),
                Integer.toString(payload.productStatus()),
                Integer.toString(payload.publishStatus()),
                Integer.toString(payload.itemBizType()),
                Long.toString(payload.price()),
                Integer.toString(payload.stock())));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long accept(Long tenantId, Long shopId, XianyuProductPushPayload payload,
                       String rawBody, String dedupeKey) {
        Long rawPayloadId = persistRedactedPayload(tenantId, payload, rawBody);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .eventType(EVENT_TYPE)
                .dedupeKey(dedupeKey)
                .externalIdentifier(payload.externalProductId())
                .processingStatus(shopId == null ? "FAILED" : "RECEIVED")
                .rawPayloadId(rawPayloadId)
                .lastErrorCode(shopId == null ? "SHOP_MAPPING_UNAVAILABLE" : null)
                .lastErrorMessage(shopId == null ? "Product push seller mapping unavailable" : null)
                .processedAt(shopId == null ? LocalDateTime.now(clock) : null)
                .build();
        event.setCreator("system");
        event.setUpdater("system");
        eventMapper.insertOrReuse(tenantId, event);
        XianyuPushEventDO existing = eventMapper.selectByTenantIdAndDedupeKeyForUpdate(tenantId, dedupeKey);
        if (existing == null) {
            throw new IllegalStateException("Product webhook event disappeared after insert");
        }
        if (shopId == null) {
            return existing.getId();
        }
        if ("FAILED".equals(existing.getProcessingStatus())) {
            existing.setProcessingStatus("RECEIVED");
            existing.setLastErrorCode(null);
            existing.setLastErrorMessage(null);
            existing.setProcessedAt(null);
            existing.setUpdater("system");
            eventMapper.updateById(existing);
            publish(tenantId, existing.getId(), shopId, payload.externalProductId());
        } else if ("RECEIVED".equals(existing.getProcessingStatus())) {
            publish(tenantId, existing.getId(), shopId, payload.externalProductId());
        }
        return existing.getId();
    }

    private Long persistRedactedPayload(Long tenantId, XianyuProductPushPayload payload, String rawBody) {
        String sourceIdentifier = "product-push:"
                + payloadHasher.sha256(payload.sellerId() + "|" + payload.externalProductId());
        String payloadHash = payloadHasher.sha256(rawBody);
        XianyuRawPayloadDO rawPayload = XianyuRawPayloadDO.builder()
                .sourceType(RAW_SOURCE_TYPE)
                .sourceIdentifier(sourceIdentifier)
                .payloadHash(payloadHash)
                .schemaVersion(RAW_SCHEMA_VERSION)
                .redactionVersion(RAW_REDACTION_VERSION)
                .payload(logRedactor.redactJson(rawBody))
                .receivedAt(LocalDateTime.now(clock))
                .build();
        rawPayload.setCreator("system");
        rawPayload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, rawPayload);
        XianyuRawPayloadDO existing = rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(
                tenantId, RAW_SOURCE_TYPE, sourceIdentifier, payloadHash);
        if (existing == null) {
            throw new IllegalStateException("Product webhook payload disappeared after insert");
        }
        return existing.getId();
    }

    private void publish(Long tenantId, Long eventId, Long shopId, String externalProductId) {
        eventPublisher.publishAfterCommitOrNow(new XianyuProductPushReceivedEvent(
                tenantId, eventId, shopId, externalProductId));
    }

}
