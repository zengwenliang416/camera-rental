package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryTraceDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalDeliveryTraceMapper extends BaseMapperX<RentalDeliveryTraceDO> {

    default List<RentalDeliveryTraceDO> selectSnapshot(Long tenantId, Long deliveryId, Integer snapshotVersion) {
        return selectList(new LambdaQueryWrapper<RentalDeliveryTraceDO>()
                .eq(RentalDeliveryTraceDO::getTenantId, tenantId)
                .eq(RentalDeliveryTraceDO::getDeliveryId, deliveryId)
                .eq(RentalDeliveryTraceDO::getSnapshotVersion, snapshotVersion)
                .orderByAsc(RentalDeliveryTraceDO::getEventSeq));
    }
}
