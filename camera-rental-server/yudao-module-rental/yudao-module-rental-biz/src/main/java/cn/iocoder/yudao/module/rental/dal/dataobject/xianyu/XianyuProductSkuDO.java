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

@TableName("xianyu_product_sku")
@KeySequence("xianyu_product_sku_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuProductSkuDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long productId;
    private String externalSkuId;
    private String skuName;
    private Integer sourceStock;
    private String status;
    private LocalDateTime sourceUpdatedAt;
    private Long rawPayloadId;

}
