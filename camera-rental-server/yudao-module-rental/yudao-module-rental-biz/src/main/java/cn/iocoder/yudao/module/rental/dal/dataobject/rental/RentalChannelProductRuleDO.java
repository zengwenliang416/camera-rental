package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("rental_channel_product_rule")
@KeySequence("rental_channel_product_rule_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalChannelProductRuleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String xianyuItemId;
    private String xgjProductId;
    private String productTitleSnapshot;
    private String handlingPolicy;
    private String mappingMode;
    private Long singleDeviceModelId;
    private Boolean enabled;
    private String ruleNote;
    private Integer lockVersion;

}
