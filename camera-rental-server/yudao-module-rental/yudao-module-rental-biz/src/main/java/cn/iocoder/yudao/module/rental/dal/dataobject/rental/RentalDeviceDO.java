package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individually traceable rental equipment.
 */
@TableName("rental_device")
@KeySequence("rental_device_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeviceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String deviceNo;
    private String serialNumber;
    private String equipmentModelCode;
    private String status;
    private String warehouseCode;
    private Integer purchaseAmount;
    private Boolean enabled;
    /** e.g. ERP_PURCHASE_IN */
    private String sourceType;
    private Long sourceBizId;
    private Long sourceItemId;

}
