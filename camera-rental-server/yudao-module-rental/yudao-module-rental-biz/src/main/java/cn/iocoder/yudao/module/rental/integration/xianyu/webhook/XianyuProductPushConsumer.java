package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class XianyuProductPushConsumer {

    private final XianyuPushEventStateService stateService;
    private final XianyuReadClient readClient;
    private final XianyuProductPersistenceService productPersistenceService;
    private final ObjectMapper objectMapper;

    public XianyuProductPushConsumer(XianyuPushEventStateService stateService,
                                     XianyuReadClient readClient,
                                     XianyuProductPersistenceService productPersistenceService,
                                     ObjectMapper objectMapper) {
        this.stateService = stateService;
        this.readClient = readClient;
        this.productPersistenceService = productPersistenceService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void onProductPush(XianyuProductPushReceivedEvent event) {
        try {
            TenantUtils.execute(event.tenantId(), () -> process(event));
        } catch (RuntimeException exception) {
            String errorCode = XianyuSafeErrorCode.from(exception);
            stateService.markRetryPreparationFailed(event.eventId(), errorCode);
            log.warn("[xianyu][webhook] product push dispatch failed eventId={} shopId={} code={}",
                    event.eventId(), event.shopId(), errorCode);
        }
    }

    private void process(XianyuProductPushReceivedEvent event) {
        String processingToken = stateService.claim(event.eventId());
        if (processingToken == null) {
            return;
        }
        try {
            String detailBody = readClient.execute(XianyuReadEndpoint.PRODUCT_DETAIL,
                    objectMapper.createObjectNode().put("product_id", Long.parseLong(event.externalProductId())))
                    .rawBody();
            productPersistenceService.persistProductDetail(event.shopId(), detailBody);
            stateService.markSucceeded(event.eventId(), processingToken);
        } catch (RuntimeException exception) {
            String errorCode = XianyuSafeErrorCode.from(exception);
            stateService.markFailed(event.eventId(), processingToken, errorCode);
            log.warn("[xianyu][webhook] product detail refresh failed eventId={} shopId={} code={}",
                    event.eventId(), event.shopId(), errorCode);
        }
    }

}
