package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceShipmentDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalDeviceShipmentMapper extends BaseMapperX<RentalDeviceShipmentDO> {

    default RentalDeviceShipmentDO selectByIdempotencyKeyForUpdate(String idempotencyKey) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceShipmentDO>()
                .eq(RentalDeviceShipmentDO::getIdempotencyKey, idempotencyKey));
    }

    default RentalDeviceShipmentDO selectByBusinessKeyForUpdate(Long channelOrderId, String waybillNo,
                                                                 String expressCode) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceShipmentDO>()
                .eq(RentalDeviceShipmentDO::getChannelOrderId, channelOrderId)
                .eq(RentalDeviceShipmentDO::getWaybillNo, waybillNo)
                .eq(RentalDeviceShipmentDO::getExpressCode, expressCode)
                .orderByDesc(RentalDeviceShipmentDO::getId)
                .last("LIMIT 1"));
    }

}
