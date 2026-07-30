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
public class XianyuProductWebhookService {

    private static final int MAX_RAW_BODY_BYTES = 65_536;

    private final XianyuWebhookSignatureVerifier signatureVerifier;
    private final XianyuProductPushPayloadParser payloadParser;
    private final XianyuProductPushShopResolver shopResolver;
    private final XianyuProductWebhookPersistenceService persistenceService;

    public XianyuProductWebhookService(XianyuWebhookSignatureVerifier signatureVerifier,
                                       XianyuProductPushPayloadParser payloadParser,
                                       XianyuProductPushShopResolver shopResolver,
                                       XianyuProductWebhookPersistenceService persistenceService) {
        this.signatureVerifier = signatureVerifier;
        this.payloadParser = payloadParser;
        this.shopResolver = shopResolver;
        this.persistenceService = persistenceService;
    }

    public XianyuWebhookReceipt receive(String appId, Long timestamp, String signature, String rawBody) {
        if (!StringUtils.hasText(appId) || timestamp == null || !StringUtils.hasText(signature)
                || !StringUtils.hasText(rawBody)
                || rawBody.getBytes(StandardCharsets.UTF_8).length > MAX_RAW_BODY_BYTES) {
            return XianyuWebhookReceipt.fail("签名失败");
        }
        XianyuProperties properties = signatureVerifier.resolveVerifiedConfig(appId, timestamp, rawBody, signature);
        if (properties == null) {
            return XianyuWebhookReceipt.fail("签名失败");
        }
        try {
            return TenantUtils.execute(properties.requireTenantId(),
                    () -> receiveWithinTenant(properties.requireTenantId(), rawBody));
        } catch (RuntimeException exception) {
            log.warn("[xianyu][webhook] product push rejected code={}", XianyuSafeErrorCode.from(exception));
            return XianyuWebhookReceipt.fail("接收失败");
        }
    }

    private XianyuWebhookReceipt receiveWithinTenant(Long tenantId, String rawBody) {
        XianyuProductPushPayload payload = payloadParser.parse(rawBody);
        Long shopId = shopResolver.resolveShopId(payload.sellerId(), payload.externalProductId());
        String dedupeKey = persistenceService.dedupeKey(payload);
        persistenceService.accept(tenantId, shopId, payload, rawBody, dedupeKey);
        return XianyuWebhookReceipt.success();
    }

}
