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
 * Restricted XianGuanJia source payload. This record must never be logged.
 */
@TableName("xianyu_raw_payload")
@KeySequence("xianyu_raw_payload_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuRawPayloadDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String sourceType;
    private String sourceIdentifier;
    private String payloadHash;
    private String schemaVersion;
    private String redactionVersion;
    private String payload;
    private LocalDateTime receivedAt;

}
