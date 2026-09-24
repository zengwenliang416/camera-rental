package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceImportBatchDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalDeviceImportBatchMapper extends BaseMapperX<RentalDeviceImportBatchDO> {
    default RentalDeviceImportBatchDO lockOwned(String id, Long userId) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceImportBatchDO>()
                .eq(RentalDeviceImportBatchDO::getId, id).eq(RentalDeviceImportBatchDO::getUserId, userId));
    }
}
