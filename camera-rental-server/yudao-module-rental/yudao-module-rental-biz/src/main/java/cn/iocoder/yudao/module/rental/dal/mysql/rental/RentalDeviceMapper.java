package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

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

    default long countBySourceItem(String sourceType, Long sourceBizId, Long sourceItemId) {
        return selectCount(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getSourceType, sourceType)
                .eq(RentalDeviceDO::getSourceBizId, sourceBizId)
                .eq(RentalDeviceDO::getSourceItemId, sourceItemId));
    }

    default RentalDeviceDO selectLatestByDeviceNoPrefix(String prefix) {
        return selectOne(new LambdaQueryWrapper<RentalDeviceDO>()
                .likeRight(RentalDeviceDO::getDeviceNo, prefix)
                .orderByDesc(RentalDeviceDO::getDeviceNo)
                .last("LIMIT 1"));
    }

}
