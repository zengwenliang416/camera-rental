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

@TableName("xianyu_product")
@KeySequence("xianyu_product_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuProductDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String xgjProductId;
    private String xianyuItemId;
    private String externalProductId;
    private String title;
    private String categoryId;
    private String status;
    private LocalDateTime sourceUpdatedAt;
    private Long rawPayloadId;

}
