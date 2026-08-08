package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceLockDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Read-only queries used by the schedule V2 allocation drawers.
 *
 * <p>The pending-order query is intentionally kept separate from the legacy
 * schedule and device controllers. It pages orders only after applying the
 * authoritative active-assignment count.</p>
 */
@Mapper
public interface RentalScheduleAllocationMapper {

    @Select("""
            <script>
            SELECT ro.*
              FROM rental_order ro
             WHERE ro.tenant_id = #{tenantId}
               AND ro.deleted = b'0'
               AND ro.status = 'PENDING_ALLOCATION'
            <if test="orderNo != null and orderNo != ''">
               AND ro.order_no LIKE CONCAT('%', #{orderNo}, '%')
            </if>
            <if test="equipmentModelCode != null and equipmentModelCode != ''">
               AND EXISTS (
                   SELECT 1
                     FROM rental_order_item filter_item
                    WHERE filter_item.tenant_id = ro.tenant_id
                      AND filter_item.rental_order_id = ro.id
                      AND filter_item.deleted = b'0'
                      AND filter_item.equipment_model_code = #{equipmentModelCode}
               )
            </if>
               AND EXISTS (
                   SELECT 1
                     FROM rental_order_item pending_item
                    WHERE pending_item.tenant_id = ro.tenant_id
                      AND pending_item.rental_order_id = ro.id
                      AND pending_item.deleted = b'0'
                      AND COALESCE(pending_item.quantity, 0) &gt; (
                          SELECT COUNT(1)
                            FROM rental_device_assignment pending_assignment
                           WHERE pending_assignment.tenant_id = pending_item.tenant_id
                             AND pending_assignment.rental_order_item_id = pending_item.id
                             AND pending_assignment.status IN ('ASSIGNED', 'DISPATCHED')
                             AND pending_assignment.deleted = b'0'
                      )
               )
             ORDER BY ro.create_time DESC, ro.id DESC
             LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<RentalOrderDO> selectPendingAllocationOrders(@Param("tenantId") Long tenantId,
                                                       @Param("orderNo") String orderNo,
                                                       @Param("equipmentModelCode") String equipmentModelCode,
                                                       @Param("offset") long offset,
                                                       @Param("pageSize") int pageSize);

    @Select("""
            <script>
            SELECT COUNT(1)
              FROM rental_order ro
             WHERE ro.tenant_id = #{tenantId}
               AND ro.deleted = b'0'
               AND ro.status = 'PENDING_ALLOCATION'
            <if test="orderNo != null and orderNo != ''">
               AND ro.order_no LIKE CONCAT('%', #{orderNo}, '%')
            </if>
            <if test="equipmentModelCode != null and equipmentModelCode != ''">
               AND EXISTS (
                   SELECT 1
                     FROM rental_order_item filter_item
                    WHERE filter_item.tenant_id = ro.tenant_id
                      AND filter_item.rental_order_id = ro.id
                      AND filter_item.deleted = b'0'
                      AND filter_item.equipment_model_code = #{equipmentModelCode}
               )
            </if>
               AND EXISTS (
                   SELECT 1
                     FROM rental_order_item pending_item
                    WHERE pending_item.tenant_id = ro.tenant_id
                      AND pending_item.rental_order_id = ro.id
                      AND pending_item.deleted = b'0'
                      AND COALESCE(pending_item.quantity, 0) &gt; (
                          SELECT COUNT(1)
                            FROM rental_device_assignment pending_assignment
                           WHERE pending_assignment.tenant_id = pending_item.tenant_id
                             AND pending_assignment.rental_order_item_id = pending_item.id
                             AND pending_assignment.status IN ('ASSIGNED', 'DISPATCHED')
                             AND pending_assignment.deleted = b'0'
                      )
               )
            </script>
            """)
    Long countPendingAllocationOrders(@Param("tenantId") Long tenantId,
                                      @Param("orderNo") String orderNo,
                                      @Param("equipmentModelCode") String equipmentModelCode);

    @Select("""
            <script>
            SELECT *
              FROM rental_device_lock
             WHERE tenant_id = #{tenantId}
               AND status = 'ACTIVE'
               AND deleted = b'0'
               AND (planned_end_time IS NULL OR planned_end_time &gt; #{now})
               AND device_id IN
            <foreach collection="deviceIds" item="deviceId" open="(" separator="," close=")">
                #{deviceId}
            </foreach>
             ORDER BY device_id ASC, id ASC
            </script>
            """)
    List<RentalDeviceLockDO> selectActiveLocks(@Param("tenantId") Long tenantId,
                                               @Param("deviceIds") Collection<Long> deviceIds,
                                               @Param("now") LocalDateTime now);
}
