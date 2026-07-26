package cn.iocoder.yudao.module.rental.integration.ocr.config;

import cn.iocoder.yudao.module.rental.integration.ocr.OpenAiCompatibleShipmentOcrClient;
import cn.iocoder.yudao.module.rental.integration.ocr.ShipmentOcrClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ShipmentOcrProperties.class)
public class ShipmentOcrConfiguration {

    @Bean
    public OkHttpClient shipmentOcrOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "yudao.ai.shipment-ocr", name = "enable", havingValue = "true")
    public ShipmentOcrClient shipmentOcrClient(ShipmentOcrProperties properties,
                                               @Qualifier("shipmentOcrOkHttpClient") OkHttpClient okHttpClient,
                                               ObjectMapper objectMapper) {
        return new OpenAiCompatibleShipmentOcrClient(properties, okHttpClient, objectMapper);
    }

}
