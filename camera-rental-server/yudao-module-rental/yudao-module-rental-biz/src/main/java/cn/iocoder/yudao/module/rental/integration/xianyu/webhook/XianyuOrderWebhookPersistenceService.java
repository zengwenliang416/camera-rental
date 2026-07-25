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
public class XianyuOrderWebhookPersistenceService {

    private static final String EVENT_TYPE = "ORDER_PUSH";
    private static final String RAW_SOURCE_TYPE = "ORDER_PUSH";
    private static final String RAW_SCHEMA_VERSION = "XIAN_GUAN_JIA_ORDER_PUSH_V1";
    private static final String RAW_REDACTION_VERSION = "REDACTED_V1";

    private final XianyuPushEventMapper eventMapper;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuPayloadHasher payloadHasher;
    private final XianyuLogRedactor logRedactor;
    private final XianyuPushEventPublisher eventPublisher;
    private final Clock clock;

    public XianyuOrderWebhookPersistenceService(XianyuPushEventMapper eventMapper,
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

    public String dedupeKey(XianyuOrderPushPayload payload) {
        return payloadHasher.sha256(String.join("|",
                EVENT_TYPE,
                payload.sellerId(),
                payload.externalOrderId(),
                Long.toString(payload.modifyTime()),
                Integer.toString(payload.orderType()),
                Integer.toString(payload.orderStatus()),
                Integer.toString(payload.refundStatus()),
                Long.toString(payload.productId()),
                Long.toString(payload.itemId())));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long accept(Long tenantId, Long shopId, XianyuOrderPushPayload payload,
                       String rawBody, String dedupeKey) {
        Long rawPayloadId = persistRedactedPayload(tenantId, payload, rawBody);
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .eventType(EVENT_TYPE)
                .dedupeKey(dedupeKey)
                .externalIdentifier(payload.externalOrderId())
                .processingStatus(shopId == null ? "FAILED" : "RECEIVED")
                .rawPayloadId(rawPayloadId)
                .lastErrorCode(shopId == null ? "SHOP_MAPPING_UNAVAILABLE" : null)
                .lastErrorMessage(shopId == null ? "Order push seller mapping unavailable" : null)
                .processedAt(shopId == null ? LocalDateTime.now(clock) : null)
                .build();
        event.setCreator("system");
        event.setUpdater("system");
        eventMapper.insertOrReuse(tenantId, event);
        XianyuPushEventDO existing = eventMapper.selectByTenantIdAndDedupeKeyForUpdate(tenantId, dedupeKey);
        if (existing == null) {
            throw new IllegalStateException("Webhook event disappeared after insert");
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
            publish(tenantId, existing.getId(), shopId, payload.externalOrderId());
        } else if ("RECEIVED".equals(existing.getProcessingStatus())) {
            publish(tenantId, existing.getId(), shopId, payload.externalOrderId());
        }
        return existing.getId();
    }

    private Long persistRedactedPayload(Long tenantId, XianyuOrderPushPayload payload, String rawBody) {
        String sourceIdentifier = "order-push:"
                + payloadHasher.sha256(payload.sellerId() + "|" + payload.externalOrderId());
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
            throw new IllegalStateException("Webhook payload disappeared after insert");
        }
        return existing.getId();
    }

    private void publish(Long tenantId, Long eventId, Long shopId, String externalOrderId) {
        eventPublisher.publishAfterCommitOrNow(new XianyuOrderPushReceivedEvent(
                tenantId, eventId, shopId, externalOrderId));
    }

}
