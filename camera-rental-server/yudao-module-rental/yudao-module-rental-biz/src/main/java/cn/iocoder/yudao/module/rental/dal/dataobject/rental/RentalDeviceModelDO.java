package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("rental_device_model")
@KeySequence("rental_device_model_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeviceModelDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long categoryId;
    private String modelCode;
    private String modelName;
    private String deviceNoPrefix;
    private Integer nextSequence;
    private Integer sortOrder;
    private Boolean enabled;
    private Integer lockVersion;

}
