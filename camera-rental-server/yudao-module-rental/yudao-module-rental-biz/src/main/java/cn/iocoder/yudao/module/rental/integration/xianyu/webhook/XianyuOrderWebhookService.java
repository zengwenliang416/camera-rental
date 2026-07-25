package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuWebhookSignatureVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class XianyuOrderWebhookService {

    private static final int MAX_RAW_BODY_BYTES = 65_536;

    private final XianyuProperties properties;
    private final XianyuWebhookSignatureVerifier signatureVerifier;
    private final XianyuOrderPushPayloadParser payloadParser;
    private final XianyuOrderPushShopResolver shopResolver;
    private final XianyuOrderWebhookPersistenceService persistenceService;

    public XianyuOrderWebhookService(XianyuProperties properties,
                                     XianyuWebhookSignatureVerifier signatureVerifier,
                                     XianyuOrderPushPayloadParser payloadParser,
                                     XianyuOrderPushShopResolver shopResolver,
                                     XianyuOrderWebhookPersistenceService persistenceService) {
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.payloadParser = payloadParser;
        this.shopResolver = shopResolver;
        this.persistenceService = persistenceService;
    }

    public XianyuWebhookReceipt receive(String appId, Long timestamp, String signature, String rawBody) {
        if (!StringUtils.hasText(appId) || timestamp == null || !StringUtils.hasText(signature)
                || !StringUtils.hasText(rawBody)
                || rawBody.getBytes(StandardCharsets.UTF_8).length > MAX_RAW_BODY_BYTES
                || !signatureVerifier.verify(appId, timestamp, rawBody, signature)) {
            return XianyuWebhookReceipt.fail("签名失败");
        }
        try {
            return TenantUtils.execute(properties.requireTenantId(), () -> receiveWithinTenant(rawBody));
        } catch (RuntimeException exception) {
            log.warn("[xianyu][webhook] order push rejected code={}", XianyuSafeErrorCode.from(exception));
            return XianyuWebhookReceipt.fail("接收失败");
        }
    }

    private XianyuWebhookReceipt receiveWithinTenant(String rawBody) {
        XianyuOrderPushPayload payload = payloadParser.parse(rawBody);
        Long shopId = shopResolver.resolveShopId(payload.sellerId(), payload.externalOrderId());
        String dedupeKey = persistenceService.dedupeKey(payload);
        persistenceService.accept(properties.requireTenantId(), shopId, payload, rawBody, dedupeKey);
        return XianyuWebhookReceipt.success();
    }

}
