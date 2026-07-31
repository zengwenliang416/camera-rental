package cn.iocoder.yudao.module.rental.dal.dataobject.logistics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("rental_delivery_device_rel")
@KeySequence("rental_delivery_device_rel_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeliveryDeviceRelDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deliveryId;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private Long assignmentId;
    private Long deviceId;
}
