package cn.iocoder.yudao.module.rental.dal.dataobject.xianyu;

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

@TableName("xianyu_push_event")
@KeySequence("xianyu_push_event_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuPushEventDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String eventType;
    private String dedupeKey;
    private String externalIdentifier;
    private String processingStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String processingToken;
    private Long rawPayloadId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastErrorCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastErrorMessage;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime processedAt;

}
