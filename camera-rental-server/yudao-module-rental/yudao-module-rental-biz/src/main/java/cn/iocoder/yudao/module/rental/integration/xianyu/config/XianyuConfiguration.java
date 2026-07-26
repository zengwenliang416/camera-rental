package cn.iocoder.yudao.module.rental.integration.xianyu.config;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuCanonicalJson;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuRequestSigner;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuWriteClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuLogRedactor;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuWebhookSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * 注册闲管家运行时配置、只读客户端与调度开关，不创建任何第三方写客户端。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(XianyuProperties.class)
@EnableScheduling
public class XianyuConfiguration {

    @Bean
    public XianyuCanonicalJson xianyuCanonicalJson(ObjectMapper objectMapper) {
        return new XianyuCanonicalJson(objectMapper);
    }

    @Bean
    public XianyuRequestSigner xianyuRequestSigner() {
        return new XianyuRequestSigner();
    }

    @Bean("xianyuClock")
    public Clock xianyuClock() {
        return Clock.systemUTC();
    }

    @Bean
    public OkHttpClient xianyuReadOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public XianyuReadClient xianyuReadClient(XianyuProperties properties, XianyuCanonicalJson canonicalJson,
                                              XianyuRequestSigner requestSigner, OkHttpClient xianyuReadOkHttpClient,
                                              ObjectMapper objectMapper,
                                              @Qualifier("xianyuClock") Clock clock) {
        return new XianyuReadClient(properties, canonicalJson, requestSigner, xianyuReadOkHttpClient,
                objectMapper, clock);
    }

    @Bean
    public XianyuWriteClient xianyuWriteClient(XianyuProperties properties, XianyuCanonicalJson canonicalJson,
                                               XianyuRequestSigner requestSigner, OkHttpClient xianyuReadOkHttpClient,
                                               ObjectMapper objectMapper,
                                               @Qualifier("xianyuClock") Clock clock) {
        return new XianyuWriteClient(properties, canonicalJson, requestSigner, xianyuReadOkHttpClient,
                objectMapper, clock);
    }

    @Bean
    public XianyuWebhookSignatureVerifier xianyuWebhookSignatureVerifier(XianyuProperties properties,
                                                                           XianyuRequestSigner requestSigner,
                                                                           @Qualifier("xianyuClock") Clock clock) {
        return new XianyuWebhookSignatureVerifier(properties, requestSigner, clock);
    }

    @Bean
    public XianyuLogRedactor xianyuLogRedactor(ObjectMapper objectMapper) {
        return new XianyuLogRedactor(objectMapper);
    }

}
