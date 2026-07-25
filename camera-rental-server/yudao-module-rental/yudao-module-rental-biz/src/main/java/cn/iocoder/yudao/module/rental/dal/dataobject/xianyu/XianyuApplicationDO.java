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

@TableName("xianyu_application")
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
    /** Opaque reference only — never store AppSecret. */
    private String credentialReference;
    private String authorizationStatus;
    private LocalDateTime authorizationExpiresAt;

}
