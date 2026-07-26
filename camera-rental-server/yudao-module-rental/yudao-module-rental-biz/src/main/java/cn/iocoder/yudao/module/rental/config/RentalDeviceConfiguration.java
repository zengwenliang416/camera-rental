package cn.iocoder.yudao.module.rental.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RentalDeviceProperties.class)
public class RentalDeviceConfiguration {
}
