package cn.iocoder.yudao.module.rental.dal.dataobject.xianyu;

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
 * Durable, redacted outcome of one bounded channel synchronization attempt.
 */
@TableName("xianyu_sync_run")
@KeySequence("xianyu_sync_run_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuSyncRunDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String resourceType;
    private String triggerType;
    private String status;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer receivedCount;
    private Integer deduplicatedCount;
    private Integer succeededCount;
    private Integer reviewRequiredCount;
    private Integer failedCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

}
