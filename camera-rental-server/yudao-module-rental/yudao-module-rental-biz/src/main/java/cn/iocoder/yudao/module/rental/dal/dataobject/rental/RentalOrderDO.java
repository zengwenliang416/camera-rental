package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Internal rental order created from a channel source only after conversion prerequisites succeed.
 */
@TableName("rental_order")
@KeySequence("rental_order_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String orderNo;
    private String sourceType;
    private String sourceOrderId;
    private Long channelOrderId;
    private String status;
    private Long rentAmount;
    private Long refundAmount;
    private LocalDateTime settledAt;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
    private LocalDate expectedSendBackDate;
    private String preparationStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String preparationReasonCode;
    private LocalDateTime preparationUpdatedAt;
    private String conversionVersion;
    private String cancelReason;

}
