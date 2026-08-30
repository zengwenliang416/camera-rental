package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RentalDeviceMapper extends BaseMapperX<RentalDeviceDO> {

    default RentalDeviceDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getId, id));
    }

    default RentalDeviceDO selectByDeviceNoForUpdate(String deviceNo) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getDeviceNo, deviceNo)
                .last("LIMIT 1"));
    }

    default RentalDeviceDO selectByDeviceNo(String deviceNo) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getDeviceNo, deviceNo)
                .last("LIMIT 1"));
    }

    default RentalDeviceDO selectBySerialNumber(String serialNumber) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getSerialNumber, serialNumber)
                .last("LIMIT 1"));
    }

    @Select("SELECT COUNT(*) FROM rental_device"
            + " WHERE tenant_id = #{tenantId} AND serial_number = #{serialNumber}"
            + " AND id <> #{deviceId}")
    @InterceptorIgnore(tenantLine = "true")
    long countAllBySerialNumberExcludingId(@Param("tenantId") Long tenantId,
                                           @Param("serialNumber") String serialNumber,
                                           @Param("deviceId") Long deviceId);

    default int updateMutableFields(Long id, String serialNumber, String warehouseCode,
                                    Integer purchaseAmount, Boolean enabled) {
        return update(new LambdaUpdateWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getId, id)
                .set(RentalDeviceDO::getSerialNumber, serialNumber)
                .set(RentalDeviceDO::getWarehouseCode, warehouseCode)
                .set(RentalDeviceDO::getPurchaseAmount, purchaseAmount)
                .set(RentalDeviceDO::getEnabled, enabled));
    }

    default RentalDeviceDO selectByLegacyDeviceNo(String legacyDeviceNo) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getLegacyDeviceNo, legacyDeviceNo)
                .last("LIMIT 1"));
    }

    default long countBySourceItem(String sourceType, Long sourceBizId, Long sourceItemId) {
        return selectCount(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getSourceType, sourceType)
                .eq(RentalDeviceDO::getSourceBizId, sourceBizId)
                .eq(RentalDeviceDO::getSourceItemId, sourceItemId));
    }

    default RentalDeviceDO selectLatestByDeviceNoPrefix(String prefix) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceDO>()
                .likeRight(RentalDeviceDO::getDeviceNo, prefix)
                .last("ORDER BY CAST(SUBSTRING_INDEX(device_no, '-', -1) AS UNSIGNED) DESC, id DESC LIMIT 1"));
    }

}
