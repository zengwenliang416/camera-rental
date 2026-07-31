package cn.iocoder.yudao.module.rental.dal.mysql.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import lombok.Data;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RentalLogisticsOperationsMapper {

    @Select("""
            SELECT id, tenant_id, delivery_id, event_type, processing_status, retry_count,
                   next_attempt_at, last_error_code, last_error_message, scheduled_at, processed_at,
                   create_time, update_time
            FROM rental_delivery_outbox
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
              AND processing_status IN ('FAILED', 'DEAD', 'RETRY_WAIT')
            ORDER BY update_time DESC, id DESC
            LIMIT #{limit}
            """)
    List<RentalDeliveryOutboxDO> selectFailedOutbox(@Param("tenantId") Long tenantId,
                                                    @Param("limit") int limit);

    @Select("""
            SELECT id, tenant_id, provider_code, delivery_id, provider_task_id, processing_status,
                   retry_count, next_retry_at, last_error_code, last_error_message, received_at,
                   processed_at, create_time, update_time
            FROM rental_delivery_callback_inbox
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
              AND processing_status IN ('FAILED', 'DEAD', 'RETRY_WAIT')
            ORDER BY update_time DESC, id DESC
            LIMIT #{limit}
            """)
    List<RentalDeliveryCallbackInboxDO> selectFailedInbox(@Param("tenantId") Long tenantId,
                                                          @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM rental_delivery_outbox
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND deleted = b'0'
            FOR UPDATE
            """)
    RentalDeliveryOutboxDO selectOutboxForUpdate(@Param("tenantId") Long tenantId,
                                                 @Param("id") Long id);

    @Select("""
            SELECT *
            FROM rental_delivery_callback_inbox
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND deleted = b'0'
            FOR UPDATE
            """)
    RentalDeliveryCallbackInboxDO selectInboxForUpdate(@Param("tenantId") Long tenantId,
                                                       @Param("id") Long id);

    @Select("""
            SELECT id
            FROM rental_delivery
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
              AND lifecycle_status = 'ACTIVE'
              AND tracking_status NOT IN ('DELIVERED', 'RETURNED')
            ORDER BY COALESCE(last_synced_at, create_time), id
            LIMIT #{limit}
            """)
    List<Long> selectReconcileCandidateIds(@Param("tenantId") Long tenantId,
                                           @Param("limit") int limit);

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM rental_delivery
                WHERE tenant_id = #{tenantId} AND deleted = b'0') AS delivery_count,
              (SELECT COUNT(*) FROM rental_delivery
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND lifecycle_status = 'ACTIVE'
                  AND tracking_status NOT IN ('DELIVERED', 'RETURNED')
                  AND (last_synced_at IS NULL OR last_synced_at < #{staleBefore})) AS stale_delivery_count,
              (SELECT COUNT(*) FROM rental_delivery_outbox
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND processing_status IN ('FAILED', 'DEAD')) AS failed_outbox_count,
              (SELECT COUNT(*) FROM rental_delivery_callback_inbox
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND processing_status IN ('FAILED', 'DEAD')) AS failed_inbox_count,
              (SELECT COUNT(*) FROM rental_delivery_outbox
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND retry_count > 0) AS retried_outbox_count,
              (SELECT COUNT(*) FROM rental_delivery_callback_inbox
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND retry_count > 0) AS retried_inbox_count,
              (SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, scheduled_at, processed_at)), 0)
                 FROM rental_delivery_outbox
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND processing_status = 'SUCCEEDED' AND processed_at IS NOT NULL) AS average_outbox_delay_seconds,
              (SELECT MAX(processed_at) FROM rental_delivery_outbox
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND processing_status = 'SUCCEEDED') AS last_outbox_success_at,
              (SELECT MAX(processed_at) FROM rental_delivery_callback_inbox
                WHERE tenant_id = #{tenantId} AND deleted = b'0'
                  AND processing_status = 'SUCCEEDED') AS last_inbox_success_at
            """)
    LogisticsMetricsRow selectMetrics(@Param("tenantId") Long tenantId,
                                      @Param("staleBefore") LocalDateTime staleBefore);

    @Select("""
            SELECT tracking_status AS status, COUNT(*) AS count
            FROM rental_delivery
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
            GROUP BY tracking_status
            ORDER BY tracking_status
            """)
    List<StatusCountRow> selectDeliveryStatusCounts(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT processing_status AS status, COUNT(*) AS count
            FROM rental_delivery_outbox
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
            GROUP BY processing_status
            ORDER BY processing_status
            """)
    List<StatusCountRow> selectOutboxStatusCounts(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT processing_status AS status, COUNT(*) AS count
            FROM rental_delivery_callback_inbox
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
            GROUP BY processing_status
            ORDER BY processing_status
            """)
    List<StatusCountRow> selectInboxStatusCounts(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT s.id AS shipment_id,
                   s.delivery_id,
                   s.channel_order_id,
                   s.assignment_id,
                   s.device_id,
                   a.rental_order_id,
                   a.rental_order_item_id,
                   xo.receiver_mobile,
                   s.waybill_no,
                   s.express_code,
                   s.express_name
            FROM rental_device_shipment s
            LEFT JOIN rental_device_assignment a
              ON a.tenant_id = s.tenant_id
             AND a.id = s.assignment_id
             AND a.deleted = b'0'
            LEFT JOIN xianyu_order xo
              ON xo.tenant_id = s.tenant_id
             AND xo.id = s.channel_order_id
             AND xo.deleted = b'0'
            WHERE s.tenant_id = #{tenantId}
              AND s.deleted = b'0'
              AND s.delivery_id IS NULL
            ORDER BY s.id
            LIMIT #{limit}
            """)
    List<BackfillCandidateRow> selectBackfillCandidates(@Param("tenantId") Long tenantId,
                                                        @Param("limit") int limit);

    @Update("""
            UPDATE rental_device_shipment
            SET delivery_id = #{deliveryId}, update_time = NOW()
            WHERE tenant_id = #{tenantId}
              AND id = #{shipmentId}
              AND delivery_id IS NULL
              AND deleted = b'0'
            """)
    int bindShipmentDelivery(@Param("tenantId") Long tenantId,
                             @Param("shipmentId") Long shipmentId,
                             @Param("deliveryId") Long deliveryId);

    @Select("""
            SELECT COUNT(*)
            FROM (
              SELECT t.id
              FROM rental_delivery_trace t
              JOIN rental_delivery d
                ON d.tenant_id = t.tenant_id
               AND d.id = t.delivery_id
               AND d.deleted = b'0'
              WHERE t.tenant_id = #{tenantId}
                AND t.deleted = b'0'
                AND t.snapshot_version < d.tracking_version
                AND t.create_time < #{cutoff}
              ORDER BY t.id
              LIMIT #{limit}
            ) eligible
            """)
    int countCleanupTraces(@Param("tenantId") Long tenantId,
                           @Param("cutoff") LocalDateTime cutoff,
                           @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM (
              SELECT id
              FROM rental_delivery_callback_inbox
              WHERE tenant_id = #{tenantId}
                AND deleted = b'0'
                AND processing_status = 'SUCCEEDED'
                AND processed_at < #{cutoff}
              ORDER BY id
              LIMIT #{limit}
            ) eligible
            """)
    int countCleanupInbox(@Param("tenantId") Long tenantId,
                          @Param("cutoff") LocalDateTime cutoff,
                          @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM (
              SELECT id
              FROM rental_delivery_outbox
              WHERE tenant_id = #{tenantId}
                AND deleted = b'0'
                AND processing_status = 'SUCCEEDED'
                AND processed_at < #{cutoff}
              ORDER BY id
              LIMIT #{limit}
            ) eligible
            """)
    int countCleanupOutbox(@Param("tenantId") Long tenantId,
                           @Param("cutoff") LocalDateTime cutoff,
                           @Param("limit") int limit);

    @Delete("""
            DELETE FROM rental_delivery_trace
            WHERE id IN (
              SELECT id FROM (
                SELECT t.id
                FROM rental_delivery_trace t
                JOIN rental_delivery d
                  ON d.tenant_id = t.tenant_id
                 AND d.id = t.delivery_id
                 AND d.deleted = b'0'
                WHERE t.tenant_id = #{tenantId}
                  AND t.deleted = b'0'
                  AND t.snapshot_version < d.tracking_version
                  AND t.create_time < #{cutoff}
                ORDER BY t.id
                LIMIT #{limit}
              ) eligible
            )
            """)
    int deleteCleanupTraces(@Param("tenantId") Long tenantId,
                            @Param("cutoff") LocalDateTime cutoff,
                            @Param("limit") int limit);

    @Delete("""
            DELETE FROM rental_delivery_callback_inbox
            WHERE id IN (
              SELECT id FROM (
                SELECT id
                FROM rental_delivery_callback_inbox
                WHERE tenant_id = #{tenantId}
                  AND deleted = b'0'
                  AND processing_status = 'SUCCEEDED'
                  AND processed_at < #{cutoff}
                ORDER BY id
                LIMIT #{limit}
              ) eligible
            )
            """)
    int deleteCleanupInbox(@Param("tenantId") Long tenantId,
                           @Param("cutoff") LocalDateTime cutoff,
                           @Param("limit") int limit);

    @Delete("""
            DELETE FROM rental_delivery_outbox
            WHERE id IN (
              SELECT id FROM (
                SELECT id
                FROM rental_delivery_outbox
                WHERE tenant_id = #{tenantId}
                  AND deleted = b'0'
                  AND processing_status = 'SUCCEEDED'
                  AND processed_at < #{cutoff}
                ORDER BY id
                LIMIT #{limit}
              ) eligible
            )
            """)
    int deleteCleanupOutbox(@Param("tenantId") Long tenantId,
                            @Param("cutoff") LocalDateTime cutoff,
                            @Param("limit") int limit);

    @Data
    class LogisticsMetricsRow {
        private Long deliveryCount;
        private Long staleDeliveryCount;
        private Long failedOutboxCount;
        private Long failedInboxCount;
        private Long retriedOutboxCount;
        private Long retriedInboxCount;
        private Long averageOutboxDelaySeconds;
        private LocalDateTime lastOutboxSuccessAt;
        private LocalDateTime lastInboxSuccessAt;
    }

    @Data
    class BackfillCandidateRow {
        private Long shipmentId;
        private Long deliveryId;
        private Long channelOrderId;
        private Long assignmentId;
        private Long deviceId;
        private Long rentalOrderId;
        private Long rentalOrderItemId;
        private String receiverMobile;
        private String waybillNo;
        private String expressCode;
        private String expressName;
    }

    @Data
    class StatusCountRow {
        private String status;
        private Long count;
    }
}
