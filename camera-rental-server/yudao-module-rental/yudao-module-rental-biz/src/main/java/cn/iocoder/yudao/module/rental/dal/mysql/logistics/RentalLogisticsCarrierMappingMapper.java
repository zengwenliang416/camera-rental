package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsCarrierMappingDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalLogisticsCarrierMappingMapper extends BaseMapperX<RentalLogisticsCarrierMappingDO> {

    default RentalLogisticsCarrierMappingDO selectEnabled(Long tenantId, String sourceType,
                                                           String sourceCarrierCode) {
        return selectOne(new LambdaQueryWrapper<RentalLogisticsCarrierMappingDO>()
                .eq(RentalLogisticsCarrierMappingDO::getTenantId, tenantId)
                .eq(RentalLogisticsCarrierMappingDO::getSourceType, sourceType)
                .eq(RentalLogisticsCarrierMappingDO::getSourceCarrierCode, sourceCarrierCode)
                .eq(RentalLogisticsCarrierMappingDO::getStatus, "ENABLED"));
    }

    default List<RentalLogisticsCarrierMappingDO> selectListByTenant(Long tenantId) {
        return selectList(new LambdaQueryWrapper<RentalLogisticsCarrierMappingDO>()
                .eq(RentalLogisticsCarrierMappingDO::getTenantId, tenantId)
                .orderByAsc(RentalLogisticsCarrierMappingDO::getSourceType)
                .orderByAsc(RentalLogisticsCarrierMappingDO::getSourceCarrierCode));
    }

    default RentalLogisticsCarrierMappingDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<RentalLogisticsCarrierMappingDO>()
                .eq(RentalLogisticsCarrierMappingDO::getTenantId, tenantId)
                .eq(RentalLogisticsCarrierMappingDO::getId, id));
    }

    default RentalLogisticsCarrierMappingDO selectBySourceForUpdate(Long tenantId, String sourceType,
                                                                    String sourceCarrierCode) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalLogisticsCarrierMappingDO>()
                .eq(RentalLogisticsCarrierMappingDO::getTenantId, tenantId)
                .eq(RentalLogisticsCarrierMappingDO::getSourceType, sourceType)
                .eq(RentalLogisticsCarrierMappingDO::getSourceCarrierCode, sourceCarrierCode));
    }
}
