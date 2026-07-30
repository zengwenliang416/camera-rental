package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class XianyuPushRetryService {

    private final XianyuRuntimeConfigService runtimeConfigService;
    private final XianyuPushEventMapper eventMapper;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuOrderPushPayloadParser orderPayloadParser;
    private final XianyuProductPushPayloadParser productPayloadParser;
    private final XianyuOrderPushShopResolver orderShopResolver;
    private final XianyuProductPushShopResolver productShopResolver;
    private final XianyuPushEventStateService stateService;
    private final XianyuPushEventPublisher eventPublisher;
    private final Clock clock;

    public XianyuPushRetryService(XianyuRuntimeConfigService runtimeConfigService,
                                  XianyuPushEventMapper eventMapper,
                                  XianyuRawPayloadMapper rawPayloadMapper,
                                  XianyuOrderPushPayloadParser orderPayloadParser,
                                  XianyuProductPushPayloadParser productPayloadParser,
                                  XianyuOrderPushShopResolver orderShopResolver,
                                  XianyuProductPushShopResolver productShopResolver,
                                  XianyuPushEventStateService stateService,
                                  XianyuPushEventPublisher eventPublisher,
                                  @Qualifier("xianyuClock") Clock clock) {
        this.runtimeConfigService = runtimeConfigService;
        this.eventMapper = eventMapper;
        this.rawPayloadMapper = rawPayloadMapper;
        this.orderPayloadParser = orderPayloadParser;
        this.productPayloadParser = productPayloadParser;
        this.orderShopResolver = orderShopResolver;
        this.productShopResolver = productShopResolver;
        this.stateService = stateService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public String retryStaleEvents() {
        XianyuProperties.Job job = runtimeConfigService.getCurrent().getJob();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime staleBefore = LocalDateTime.now(clock)
                .minusSeconds(Math.max(30, job.getPushRetryStaleSeconds()));
        List<XianyuPushEventDO> candidates = eventMapper.selectRetryCandidates(
                staleBefore, job.getPushRetryBatchSize());
        int queued = 0;
        int failed = 0;
        for (XianyuPushEventDO event : candidates) {
            if (!stateService.prepareRetry(event.getId(), staleBefore)) {
                continue;
            }
            try {
                XianyuRawPayloadDO rawPayload = rawPayloadMapper.selectByTenantIdAndId(
                        tenantId, event.getRawPayloadId());
                if (rawPayload == null) {
                    throw new IllegalStateException("XianGuanJia push raw payload is missing");
                }
                publishReplayEvent(event, rawPayload);
                queued++;
            } catch (RuntimeException exception) {
                failed++;
                String errorCode = XianyuSafeErrorCode.from(exception);
                stateService.markRetryPreparationFailed(event.getId(), errorCode);
                log.warn("[xianyu][webhook-retry] event retry preparation failed eventId={} code={}",
                        event.getId(), errorCode);
            }
        }
        return "candidates=" + candidates.size() + " queued=" + queued + " failed=" + failed;
    }

    public XianyuPushReplayOutcome replayPushEvent(Long eventId, Long operatorId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        XianyuPushEventDO event = eventMapper.selectByTenantIdAndId(tenantId, eventId);
        if (event == null) {
            return XianyuPushReplayOutcome.failed(eventId, "EVENT_NOT_FOUND");
        }
        if (!stateService.prepareManualReplay(eventId, operatorId == null ? "system" : String.valueOf(operatorId))) {
            return XianyuPushReplayOutcome.skipped(eventId, event.getProcessingStatus());
        }
        try {
            XianyuRawPayloadDO rawPayload = rawPayloadMapper.selectByTenantIdAndId(
                    tenantId, event.getRawPayloadId());
            if (rawPayload == null) {
                throw new IllegalStateException("XianGuanJia push raw payload is missing");
            }
            publishReplayEvent(event, rawPayload);
            return XianyuPushReplayOutcome.queued(eventId);
        } catch (RuntimeException exception) {
            String errorCode = XianyuSafeErrorCode.from(exception);
            stateService.markRetryPreparationFailed(eventId, errorCode);
            log.warn("[xianyu][webhook-replay] event replay preparation failed eventId={} code={}",
                    eventId, errorCode);
            return XianyuPushReplayOutcome.failed(eventId, errorCode);
        }
    }

    private void publishReplayEvent(XianyuPushEventDO event, XianyuRawPayloadDO rawPayload) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (XianyuProductWebhookPersistenceService.EVENT_TYPE.equals(event.getEventType())) {
            ProductReplayTarget target = resolveProductReplayTarget(event, rawPayload);
            Long shopId = productShopResolver.resolveShopId(target.sellerId(), target.externalProductId());
            if (shopId == null) {
                throw new IllegalStateException("XianGuanJia product push seller mapping is missing or ambiguous");
            }
            eventPublisher.publishAfterCommitOrNow(new XianyuProductPushReceivedEvent(
                    tenantId, event.getId(), shopId, target.externalProductId()));
            return;
        }
        if (event.getEventType() == null || "ORDER_PUSH".equals(event.getEventType())) {
            XianyuOrderPushPayload payload = orderPayloadParser.parse(rawPayload.getPayload());
            Long shopId = orderShopResolver.resolveShopId(payload.sellerId(), payload.externalOrderId());
            if (shopId == null) {
                throw new IllegalStateException("XianGuanJia order push seller mapping is missing or ambiguous");
            }
            eventPublisher.publishAfterCommitOrNow(new XianyuOrderPushReceivedEvent(
                    tenantId, event.getId(), shopId, payload.externalOrderId()));
            return;
        }
        throw new IllegalStateException("Unsupported XianGuanJia push event type");
    }

    private ProductReplayTarget resolveProductReplayTarget(XianyuPushEventDO event, XianyuRawPayloadDO rawPayload) {
        try {
            XianyuProductPushPayload payload = productPayloadParser.parse(rawPayload.getPayload());
            return new ProductReplayTarget(payload.sellerId(), payload.externalProductId());
        } catch (RuntimeException ignored) {
            if (!StringUtils.hasText(event.getExternalIdentifier())) {
                throw new IllegalStateException("XianGuanJia product push payload cannot be replayed");
            }
            return new ProductReplayTarget(null, event.getExternalIdentifier());
        }
    }

    private record ProductReplayTarget(String sellerId, String externalProductId) {
    }

}
