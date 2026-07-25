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

}
