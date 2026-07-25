package cn.iocoder.yudao.module.rental.dal.dataobject.xianyu;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Explicit mapping from one channel product/SKU to an internal equipment model.
 */
@TableName("xianyu_product_mapping")
@KeySequence("xianyu_product_mapping_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuProductMappingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String externalProductId;
    private String externalSkuId;
    private String equipmentModelCode;
    private String mappingStatus;
    private String mappingNote;

}
