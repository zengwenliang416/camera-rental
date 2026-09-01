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

import java.time.LocalDateTime;

@TableName("rental_historical_reconciliation_run")
@KeySequence("rental_historical_reconciliation_run_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalHistoricalReconciliationRunDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Boolean dryRun;
    private String status;
    private Long startAfterId;
    private Long endIdInclusive;
    private Long cursorAfterId;
    private Integer batchSize;
    private Integer resumeCount;
    private Integer scannedCount;
    private Integer skippedCount;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer unchangedCount;
    private Integer conflictCount;
    private Integer failedCount;
    private Integer reviewRequiredCount;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long lastFailedOrderId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastErrorCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String executionToken;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime leaseUntil;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime heartbeatAt;
    private LocalDateTime startedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime pausedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime finishedAt;

}
