package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalDeliveryDeviceRelMapper extends BaseMapperX<RentalDeliveryDeviceRelDO> {

    default RentalDeliveryDeviceRelDO selectByDeliveryAndDeviceForUpdate(Long tenantId, Long deliveryId,
                                                                          Long deviceId) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeliveryDeviceRelDO>()
                .eq(RentalDeliveryDeviceRelDO::getTenantId, tenantId)
                .eq(RentalDeliveryDeviceRelDO::getDeliveryId, deliveryId)
                .eq(RentalDeliveryDeviceRelDO::getDeviceId, deviceId));
    }

    default List<RentalDeliveryDeviceRelDO> selectListByDeliveryId(Long tenantId, Long deliveryId) {
        return selectList(new LambdaQueryWrapper<RentalDeliveryDeviceRelDO>()
                .eq(RentalDeliveryDeviceRelDO::getTenantId, tenantId)
                .eq(RentalDeliveryDeviceRelDO::getDeliveryId, deliveryId)
                .orderByAsc(RentalDeliveryDeviceRelDO::getId));
    }
}
