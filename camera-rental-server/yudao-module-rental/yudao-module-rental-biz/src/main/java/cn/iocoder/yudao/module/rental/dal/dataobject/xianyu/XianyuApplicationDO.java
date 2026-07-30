package cn.iocoder.yudao.module.rental.dal.dataobject.xianyu;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "xianyu_application", autoResultMap = true)
@KeySequence("xianyu_application_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuApplicationDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String applicationCode;
    private String displayName;
    private Boolean enabled;
    private String baseUrl;
    private String appKey;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String appSecret;
    private String webhookBaseUrl;
    private Boolean writeEnabled;
    private Boolean jobEnabled;
    private Integer lookbackDays;
    private Integer overlapMinutes;
    private Integer maxPagesPerShop;
    private Integer pageSize;
    private Integer pushRetryStaleSeconds;
    private Integer pushRetryBatchSize;
    private String authorizationStatus;
    private LocalDateTime authorizationExpiresAt;

}
