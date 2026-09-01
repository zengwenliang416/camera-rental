package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("rental_device_category")
@KeySequence("rental_device_category_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeviceCategoryDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String categoryCode;
    private String categoryName;
    private Integer sortOrder;
    private Boolean enabled;
    private Integer lockVersion;

}
