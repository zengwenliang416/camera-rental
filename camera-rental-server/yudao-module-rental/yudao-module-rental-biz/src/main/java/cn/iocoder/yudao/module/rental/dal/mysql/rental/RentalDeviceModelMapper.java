package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalDeviceModelMapper extends BaseMapperX<RentalDeviceModelDO> {

    default List<RentalDeviceModelDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapper<RentalDeviceModelDO>()
                .eq(RentalDeviceModelDO::getEnabled, true)
                .orderByAsc(RentalDeviceModelDO::getSortOrder)
                .orderByAsc(RentalDeviceModelDO::getId));
    }

    default List<RentalDeviceModelDO> selectConfigurationList() {
        return selectList(new LambdaQueryWrapper<RentalDeviceModelDO>()
                .orderByAsc(RentalDeviceModelDO::getSortOrder)
                .orderByAsc(RentalDeviceModelDO::getId));
    }

    default RentalDeviceModelDO selectByCode(String modelCode) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceModelDO>()
                .eq(RentalDeviceModelDO::getModelCode, modelCode)
                .last("LIMIT 1"));
    }

    default RentalDeviceModelDO selectByPrefix(String deviceNoPrefix) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceModelDO>()
                .eq(RentalDeviceModelDO::getDeviceNoPrefix, deviceNoPrefix)
                .last("LIMIT 1"));
    }

    default RentalDeviceModelDO selectByCategoryAndCode(Long categoryId, String modelCode) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceModelDO>()
                .eq(RentalDeviceModelDO::getCategoryId, categoryId)
                .eq(RentalDeviceModelDO::getModelCode, modelCode)
                .last("LIMIT 1"));
    }

    default RentalDeviceModelDO selectByCategoryAndCodeForUpdate(Long categoryId, String modelCode) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceModelDO>()
                .eq(RentalDeviceModelDO::getCategoryId, categoryId)
                .eq(RentalDeviceModelDO::getModelCode, modelCode)
                .last("LIMIT 1"));
    }

    default RentalDeviceModelDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceModelDO>()
                .eq(RentalDeviceModelDO::getId, id));
    }

    default int updateByIdAndVersion(RentalDeviceModelDO model, Long tenantId, Integer expectedVersion) {
        return update(model, new LambdaUpdateWrapper<RentalDeviceModelDO>()
                .eq(RentalDeviceModelDO::getId, model.getId())
                .eq(RentalDeviceModelDO::getTenantId, tenantId)
                .eq(RentalDeviceModelDO::getLockVersion, expectedVersion));
    }

}
