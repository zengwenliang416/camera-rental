package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Half-open occupied period for a physical rental device.
 */
@TableName("rental_schedule")
@KeySequence("rental_schedule_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalScheduleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String scheduleType;
    private String status;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
    private String idempotencyKey;

}
