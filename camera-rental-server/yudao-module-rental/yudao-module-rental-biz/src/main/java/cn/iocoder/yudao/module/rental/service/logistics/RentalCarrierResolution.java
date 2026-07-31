package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsCarrierMappingDO;

public record RentalCarrierResolution(
        String sourceType,
        String sourceCarrierCode,
        String canonicalCarrierCode,
        RentalLogisticsCarrierMappingDO mapping
) {
}
