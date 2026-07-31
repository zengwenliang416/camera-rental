package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RentalDeliveryCallbackInboxMapper extends BaseMapperX<RentalDeliveryCallbackInboxDO> {

    @Insert("""
            INSERT INTO rental_delivery_callback_inbox (
                tenant_id, provider_code, delivery_id, provider_task_id, payload_hash,
                callback_params, processing_status, processing_token, lease_until, retry_count,
                next_retry_at, last_error_code, last_error_message, received_at, processed_at,
                creator, create_time, updater, update_time, deleted
            ) VALUES (
                #{tenantId}, #{inbox.providerCode}, #{inbox.deliveryId}, #{inbox.providerTaskId},
                #{inbox.payloadHash},
                #{inbox.callbackParams,typeHandler=cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler},
                #{inbox.processingStatus}, #{inbox.processingToken}, #{inbox.leaseUntil},
                #{inbox.retryCount}, #{inbox.nextRetryAt}, #{inbox.lastErrorCode},
                #{inbox.lastErrorMessage}, #{inbox.receivedAt}, #{inbox.processedAt},
                '', NOW(), '', NOW(), b'0'
            )
            ON DUPLICATE KEY UPDATE id = id
            """)
    void insertOrReuse(@Param("tenantId") Long tenantId,
                       @Param("inbox") RentalDeliveryCallbackInboxDO inbox);

    default RentalDeliveryCallbackInboxDO selectByPayloadHashForUpdate(Long tenantId, String providerCode,
                                                                        Long deliveryId, String payloadHash) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalDeliveryCallbackInboxDO>()
                .eq(RentalDeliveryCallbackInboxDO::getTenantId, tenantId)
                .eq(RentalDeliveryCallbackInboxDO::getProviderCode, providerCode)
                .eq(RentalDeliveryCallbackInboxDO::getDeliveryId, deliveryId)
                .eq(RentalDeliveryCallbackInboxDO::getPayloadHash, payloadHash));
    }

    @Select("""
            SELECT *
            FROM rental_delivery_callback_inbox
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
              AND (
                (processing_status IN ('RECEIVED', 'RETRY_WAIT')
                  AND (next_retry_at IS NULL OR next_retry_at <= #{now}))
                OR (processing_status = 'PROCESSING' AND lease_until < #{now})
              )
            ORDER BY id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<RentalDeliveryCallbackInboxDO> selectClaimableForUpdate(@Param("tenantId") Long tenantId,
                                                                 @Param("now") LocalDateTime now,
                                                                 @Param("limit") int limit);
}
