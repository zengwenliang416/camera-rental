package cn.iocoder.yudao.module.rental.dal.dataobject.logistics;

import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
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

@TableName(value = "rental_logistics_provider_credential", autoResultMap = true)
@KeySequence("rental_logistics_provider_credential_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalLogisticsProviderCredentialDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String providerCode;
    private String credentialName;
    private Boolean enabled;
    private Integer sortOrder;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String customerCode;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String apiKey;
    private String configStatus;
    private LocalDateTime lastVerifiedAt;
}
