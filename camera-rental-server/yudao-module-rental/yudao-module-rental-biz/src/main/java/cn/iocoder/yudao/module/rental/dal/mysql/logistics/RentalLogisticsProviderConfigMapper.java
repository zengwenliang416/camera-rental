package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalLogisticsProviderConfigMapper extends BaseMapperX<RentalLogisticsProviderConfigDO> {

    default RentalLogisticsProviderConfigDO selectByProviderCode(Long tenantId, String providerCode) {
        return selectOne(new LambdaQueryWrapper<RentalLogisticsProviderConfigDO>()
                .eq(RentalLogisticsProviderConfigDO::getTenantId, tenantId)
                .eq(RentalLogisticsProviderConfigDO::getProviderCode, providerCode));
    }

    default RentalLogisticsProviderConfigDO selectByProviderCodeForUpdate(Long tenantId, String providerCode) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalLogisticsProviderConfigDO>()
                .eq(RentalLogisticsProviderConfigDO::getTenantId, tenantId)
                .eq(RentalLogisticsProviderConfigDO::getProviderCode, providerCode));
    }
}
