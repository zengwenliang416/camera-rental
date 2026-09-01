package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("rental_historical_reconciliation_failure")
@KeySequence("rental_historical_reconciliation_failure_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalHistoricalReconciliationFailureDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long runId;
    private Long channelOrderId;
    private Long cursorBeforeId;
    private Integer attemptNo;
    private String errorCode;

}

