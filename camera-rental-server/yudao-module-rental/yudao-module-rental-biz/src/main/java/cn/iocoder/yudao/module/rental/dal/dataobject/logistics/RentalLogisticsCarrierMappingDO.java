package cn.iocoder.yudao.module.rental.dal.dataobject.logistics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("rental_logistics_carrier_mapping")
@KeySequence("rental_logistics_carrier_mapping_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalLogisticsCarrierMappingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String sourceType;
    private String sourceCarrierCode;
    private String canonicalCarrierCode;
    private String displayName;
    private String providerCode;
    private String providerCarrierCode;
    private String phoneRequirement;
    private String status;
}
