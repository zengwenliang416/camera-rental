package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalLogisticsProviderCredentialMapper
        extends BaseMapperX<RentalLogisticsProviderCredentialDO> {

    default RentalLogisticsProviderCredentialDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<RentalLogisticsProviderCredentialDO>()
                .eq(RentalLogisticsProviderCredentialDO::getTenantId, tenantId)
                .eq(RentalLogisticsProviderCredentialDO::getId, id));
    }

    default RentalLogisticsProviderCredentialDO selectByTenantIdAndIdForUpdate(Long tenantId, Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalLogisticsProviderCredentialDO>()
                .eq(RentalLogisticsProviderCredentialDO::getTenantId, tenantId)
                .eq(RentalLogisticsProviderCredentialDO::getId, id));
    }

    default RentalLogisticsProviderCredentialDO selectByNameForUpdate(
            Long tenantId, String providerCode, String credentialName) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalLogisticsProviderCredentialDO>()
                .eq(RentalLogisticsProviderCredentialDO::getTenantId, tenantId)
                .eq(RentalLogisticsProviderCredentialDO::getProviderCode, providerCode)
                .eq(RentalLogisticsProviderCredentialDO::getCredentialName, credentialName));
    }

    default List<RentalLogisticsProviderCredentialDO> selectListByProvider(
            Long tenantId, String providerCode) {
        return selectList(new LambdaQueryWrapper<RentalLogisticsProviderCredentialDO>()
                .eq(RentalLogisticsProviderCredentialDO::getTenantId, tenantId)
                .eq(RentalLogisticsProviderCredentialDO::getProviderCode, providerCode)
                .orderByAsc(RentalLogisticsProviderCredentialDO::getSortOrder)
                .orderByAsc(RentalLogisticsProviderCredentialDO::getId));
    }
}
