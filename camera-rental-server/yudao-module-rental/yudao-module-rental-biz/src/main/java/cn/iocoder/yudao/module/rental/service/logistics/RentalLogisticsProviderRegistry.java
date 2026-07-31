package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RentalLogisticsProviderRegistry {

    private final Map<String, LogisticsProvider> providers;

    public RentalLogisticsProviderRegistry(List<LogisticsProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(LogisticsProvider::providerCode, Function.identity()));
    }

    public LogisticsProvider require(String providerCode) {
        LogisticsProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new RentalLogisticsException("PROVIDER_NOT_FOUND");
        }
        return provider;
    }
}
