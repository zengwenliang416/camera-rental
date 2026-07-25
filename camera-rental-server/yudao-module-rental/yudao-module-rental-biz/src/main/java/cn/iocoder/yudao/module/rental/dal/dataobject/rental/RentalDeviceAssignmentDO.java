package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Links one rental order item to its assigned physical device and occupied schedule.
 */
@TableName("rental_device_assignment")
@KeySequence("rental_device_assignment_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeviceAssignmentDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private Long deviceId;
    private Long scheduleId;
    private String status;
    private String idempotencyKey;
    private LocalDateTime assignedAt;

}
