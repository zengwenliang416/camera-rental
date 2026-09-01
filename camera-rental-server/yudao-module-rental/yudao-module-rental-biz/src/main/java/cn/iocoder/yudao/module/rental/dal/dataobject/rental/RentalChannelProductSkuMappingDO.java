package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("rental_channel_product_sku_mapping")
@KeySequence("rental_channel_product_sku_mapping_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalChannelProductSkuMappingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long productRuleId;
    private Long productSkuId;
    private String xgjSkuId;
    private String xianyuSkuId;
    private Long deviceModelId;
    private Boolean enabled;
    private Integer lockVersion;

}
