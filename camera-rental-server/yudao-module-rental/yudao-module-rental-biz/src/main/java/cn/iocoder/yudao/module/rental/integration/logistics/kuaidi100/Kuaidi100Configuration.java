package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class Kuaidi100Configuration {

    @Bean("kuaidi100OkHttpClient")
    public OkHttpClient kuaidi100OkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .callTimeout(Duration.ofSeconds(15))
                .build();
    }
}
