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

@TableName("xianyu_alert")
@KeySequence("xianyu_alert_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuAlertDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String alertType;
    private String dedupeKey;
    private String severity;
    private String status;
    private String sourceIdentifier;
    private String message;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime resolvedAt;

}
