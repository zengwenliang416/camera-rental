package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RentalDeliveryOutboxMapper extends BaseMapperX<RentalDeliveryOutboxDO> {

    @Insert("""
            INSERT INTO rental_delivery_outbox (
                tenant_id, delivery_id, event_type, dedupe_key, safe_metadata,
                processing_status, processing_token, lease_until, retry_count, next_attempt_at,
                last_error_code, last_error_message, scheduled_at, processed_at,
                creator, create_time, updater, update_time, deleted
            ) VALUES (
                #{tenantId}, #{outbox.deliveryId}, #{outbox.eventType}, #{outbox.dedupeKey},
                #{outbox.safeMetadata}, #{outbox.processingStatus}, #{outbox.processingToken},
                #{outbox.leaseUntil}, #{outbox.retryCount}, #{outbox.nextAttemptAt},
                #{outbox.lastErrorCode}, #{outbox.lastErrorMessage}, #{outbox.scheduledAt},
                #{outbox.processedAt}, '', NOW(), '', NOW(), b'0'
            )
            ON DUPLICATE KEY UPDATE id = id
            """)
    void insertOrReuse(@Param("tenantId") Long tenantId,
                       @Param("outbox") RentalDeliveryOutboxDO outbox);

    default RentalDeliveryOutboxDO selectByDedupeKeyForUpdate(Long tenantId, String dedupeKey) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeliveryOutboxDO>()
                .eq(RentalDeliveryOutboxDO::getTenantId, tenantId)
                .eq(RentalDeliveryOutboxDO::getDedupeKey, dedupeKey));
    }

    default List<RentalDeliveryOutboxDO> selectPendingByDeliveryId(Long tenantId, Long deliveryId) {
        return selectList(new LambdaQueryWrapper<RentalDeliveryOutboxDO>()
                .eq(RentalDeliveryOutboxDO::getTenantId, tenantId)
                .eq(RentalDeliveryOutboxDO::getDeliveryId, deliveryId)
                .in(RentalDeliveryOutboxDO::getProcessingStatus, "PENDING", "PROCESSING", "RETRY_WAIT")
                .orderByAsc(RentalDeliveryOutboxDO::getId));
    }

    @Select("""
            SELECT *
            FROM rental_delivery_outbox
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
              AND (
                (processing_status IN ('PENDING', 'RETRY_WAIT')
                  AND (next_attempt_at IS NULL OR next_attempt_at <= #{now}))
                OR (processing_status = 'PROCESSING' AND lease_until < #{now})
              )
            ORDER BY id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<RentalDeliveryOutboxDO> selectClaimableForUpdate(@Param("tenantId") Long tenantId,
                                                          @Param("now") LocalDateTime now,
                                                          @Param("limit") int limit);
}
