package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class XianyuOrderPushConsumer {

    private final XianyuPushEventStateService stateService;
    private final XianyuReadClient readClient;
    private final XianyuOrderPersistenceService orderPersistenceService;
    private final ObjectMapper objectMapper;

    public XianyuOrderPushConsumer(XianyuPushEventStateService stateService,
                                   XianyuReadClient readClient,
                                   XianyuOrderPersistenceService orderPersistenceService,
                                   ObjectMapper objectMapper) {
        this.stateService = stateService;
        this.readClient = readClient;
        this.orderPersistenceService = orderPersistenceService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void onOrderPush(XianyuOrderPushReceivedEvent event) {
        try {
            TenantUtils.execute(event.tenantId(), () -> process(event));
        } catch (RuntimeException exception) {
            String errorCode = XianyuSafeErrorCode.from(exception);
            stateService.markRetryPreparationFailed(event.eventId(), errorCode);
            log.warn("[xianyu][webhook] order push dispatch failed eventId={} shopId={} code={}",
                    event.eventId(), event.shopId(), errorCode);
        }
    }

    private void process(XianyuOrderPushReceivedEvent event) {
        String processingToken = stateService.claim(event.eventId());
        if (processingToken == null) {
            return;
        }
        try {
            String detailBody = readClient.execute(XianyuReadEndpoint.ORDER_DETAIL,
                    objectMapper.createObjectNode().put("order_no", event.externalOrderId())).rawBody();
            orderPersistenceService.persistOrderDetail(event.shopId(), detailBody);
            stateService.markSucceeded(event.eventId(), processingToken);
        } catch (RuntimeException exception) {
            String errorCode = XianyuSafeErrorCode.from(exception);
            stateService.markFailed(event.eventId(), processingToken, errorCode);
            log.warn("[xianyu][webhook] order detail refresh failed eventId={} shopId={} code={}",
                    event.eventId(), event.shopId(), errorCode);
        }
    }

}
