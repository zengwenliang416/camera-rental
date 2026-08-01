package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RentalDeliveryMapper extends BaseMapperX<RentalDeliveryDO> {

    default RentalDeliveryDO selectByBusinessKeyForUpdate(Long tenantId, Long rentalOrderId, String direction,
                                                           String canonicalCarrierCode, String normalizedWaybillNo) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeliveryDO>()
                .eq(RentalDeliveryDO::getTenantId, tenantId)
                .eq(RentalDeliveryDO::getRentalOrderId, rentalOrderId)
                .eq(RentalDeliveryDO::getDirection, direction)
                .eq(RentalDeliveryDO::getCanonicalCarrierCode, canonicalCarrierCode)
                .eq(RentalDeliveryDO::getNormalizedWaybillNo, normalizedWaybillNo));
    }

    default RentalDeliveryDO selectByReferenceBusinessKeyForUpdate(
            Long tenantId, Long rentalOrderId, Long channelOrderId, String direction,
            String canonicalCarrierCode, String normalizedWaybillNo) {
        LambdaQueryWrapper<RentalDeliveryDO> wrapper = new LambdaQueryWrapper<RentalDeliveryDO>()
                .eq(RentalDeliveryDO::getTenantId, tenantId)
                .eq(RentalDeliveryDO::getDirection, direction)
                .eq(RentalDeliveryDO::getCanonicalCarrierCode, canonicalCarrierCode)
                .eq(RentalDeliveryDO::getNormalizedWaybillNo, normalizedWaybillNo);
        if (channelOrderId != null) {
            wrapper.eq(RentalDeliveryDO::getChannelOrderId, channelOrderId);
        } else {
            wrapper.eq(RentalDeliveryDO::getRentalOrderId, rentalOrderId);
        }
        return selectOneForUpdate(wrapper);
    }

    default RentalDeliveryDO selectByTenantIdAndIdForUpdate(Long tenantId, Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeliveryDO>()
                .eq(RentalDeliveryDO::getTenantId, tenantId)
                .eq(RentalDeliveryDO::getId, id));
    }

    default RentalDeliveryDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<RentalDeliveryDO>()
                .eq(RentalDeliveryDO::getTenantId, tenantId)
                .eq(RentalDeliveryDO::getId, id));
    }

    default List<RentalDeliveryDO> selectCallbackCandidatesByTokenHash(String callbackTokenHash) {
        return selectList(new LambdaQueryWrapper<RentalDeliveryDO>()
                .select(RentalDeliveryDO::getId, RentalDeliveryDO::getTenantId,
                        RentalDeliveryDO::getCallbackToken, RentalDeliveryDO::getCallbackSalt)
                .eq(RentalDeliveryDO::getCallbackTokenHash, callbackTokenHash)
                .orderByAsc(RentalDeliveryDO::getTenantId)
                .orderByAsc(RentalDeliveryDO::getId));
    }

    default List<RentalDeliveryDO> selectCompensationCandidates(Long tenantId, LocalDateTime staleBefore,
                                                                 int limit) {
        return selectList(new LambdaQueryWrapper<RentalDeliveryDO>()
                .eq(RentalDeliveryDO::getTenantId, tenantId)
                .eq(RentalDeliveryDO::getLifecycleStatus, "ACTIVE")
                .notIn(RentalDeliveryDO::getTrackingStatus, "DELIVERED", "RETURNED")
                .and(wrapper -> wrapper.isNull(RentalDeliveryDO::getLastSyncedAt)
                        .or().lt(RentalDeliveryDO::getLastSyncedAt, staleBefore))
                .orderByAsc(RentalDeliveryDO::getLastSyncedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    @Select("""
            SELECT COALESCE(MAX(package_seq), 0)
            FROM rental_delivery
            WHERE tenant_id = #{tenantId}
              AND rental_order_id = #{rentalOrderId}
              AND direction = #{direction}
              AND deleted = b'0'
            """)
    Integer selectMaxPackageSeq(@Param("tenantId") Long tenantId,
                                @Param("rentalOrderId") Long rentalOrderId,
                                @Param("direction") String direction);

    @Select("""
            SELECT COALESCE(MAX(package_seq), 0)
            FROM rental_delivery
            WHERE tenant_id = #{tenantId}
              AND channel_order_id = #{channelOrderId}
              AND direction = #{direction}
              AND deleted = b'0'
            """)
    Integer selectMaxChannelPackageSeq(@Param("tenantId") Long tenantId,
                                       @Param("channelOrderId") Long channelOrderId,
                                       @Param("direction") String direction);
}
