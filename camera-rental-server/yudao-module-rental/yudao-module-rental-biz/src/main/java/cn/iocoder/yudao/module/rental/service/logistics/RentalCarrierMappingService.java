package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsCarrierMappingDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsCarrierMappingMapper;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RentalCarrierMappingService {

    private final RentalLogisticsCarrierMappingMapper mappingMapper;

    public RentalCarrierMappingService(RentalLogisticsCarrierMappingMapper mappingMapper) {
        this.mappingMapper = mappingMapper;
    }

    public RentalCarrierResolution resolve(String sourceType, String sourceCarrierCode) {
        String normalizedSourceType = normalizeCode(sourceType);
        String normalizedSourceCarrierCode = normalizeCode(sourceCarrierCode);
        RentalLogisticsCarrierMappingDO mapping = mappingMapper.selectEnabled(
                TenantContextHolder.getRequiredTenantId(), normalizedSourceType, normalizedSourceCarrierCode);
        String canonicalCarrierCode = mapping == null
                ? normalizedSourceCarrierCode
                : normalizeCode(mapping.getCanonicalCarrierCode());
        return new RentalCarrierResolution(normalizedSourceType, normalizedSourceCarrierCode,
                canonicalCarrierCode, mapping);
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
