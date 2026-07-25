package cn.iocoder.yudao.module.rental.integration.xianyu.security;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuRequestSigner;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;

import java.time.Clock;

/**
 * Verifies the documented order-push signature against the received raw body.
 */
public class XianyuWebhookSignatureVerifier {

    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300;

    private final XianyuProperties properties;
    private final XianyuRequestSigner requestSigner;
    private final Clock clock;

    public XianyuWebhookSignatureVerifier(XianyuProperties properties, XianyuRequestSigner requestSigner, Clock clock) {
        this.properties = properties;
        this.requestSigner = requestSigner;
        this.clock = clock;
    }

    public boolean verify(String appId, long timestamp, String rawBody, String signature) {
        if (properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY
                || rawBody == null
                || signature == null
                || !properties.getAppKey().equals(appId)
                || Math.abs(clock.instant().getEpochSecond() - timestamp) > MAX_TIMESTAMP_SKEW_SECONDS) {
            return false;
        }
        return requestSigner.matches(properties.getAppKey(), properties.getAppSecret(), timestamp, rawBody, signature);
    }

}
