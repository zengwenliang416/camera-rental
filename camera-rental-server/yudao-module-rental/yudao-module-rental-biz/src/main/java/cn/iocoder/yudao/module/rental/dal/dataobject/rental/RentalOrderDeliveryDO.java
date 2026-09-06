package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Per-order delivery facts for manually created offline orders (1:0..1 with rental_order).
 * Receiver contact is encrypted at rest.
 */
@TableName(value = "rental_order_delivery", autoResultMap = true)
@KeySequence("rental_order_delivery_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderDeliveryDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long rentalOrderId;
    private String deliveryMethod;
    private String receiverName;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String receiverMobile;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String receiverAddress;
    private String deliveryRemark;

}
