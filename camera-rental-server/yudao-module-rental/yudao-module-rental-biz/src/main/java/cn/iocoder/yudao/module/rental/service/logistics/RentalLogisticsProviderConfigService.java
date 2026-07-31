package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class RentalLogisticsProviderConfigService {

    public static final int MINIMUM_QUERY_INTERVAL_SECONDS = 1800;
    private final RentalLogisticsProviderConfigMapper configMapper;

    public RentalLogisticsProviderConfigService(RentalLogisticsProviderConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public RentalLogisticsProviderConfigDO get(String providerCode) {
        return configMapper.selectByProviderCode(TenantContextHolder.getRequiredTenantId(), providerCode);
    }

    public boolean isProviderEnabled(String providerCode) {
        RentalLogisticsProviderConfigDO config = get(providerCode);
        return config != null && Boolean.TRUE.equals(config.getEnabled());
    }

    public int minimumQueryIntervalSeconds(String providerCode) {
        RentalLogisticsProviderConfigDO config = get(providerCode);
        if (config == null || config.getMinimumQueryIntervalSeconds() == null) {
            return MINIMUM_QUERY_INTERVAL_SECONDS;
        }
        return Math.max(MINIMUM_QUERY_INTERVAL_SECONDS, config.getMinimumQueryIntervalSeconds());
    }
}
