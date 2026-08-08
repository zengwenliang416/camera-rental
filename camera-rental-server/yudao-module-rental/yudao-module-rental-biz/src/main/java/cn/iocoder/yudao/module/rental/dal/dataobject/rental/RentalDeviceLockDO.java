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

@TableName("rental_device_lock")
@KeySequence("rental_device_lock_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeviceLockDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private String lockType;
    private String reason;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String sourceType;
    private LocalDateTime startTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime releasedAt;
    private Long releasedBy;
    private String releaseReason;
    private String status;
}
