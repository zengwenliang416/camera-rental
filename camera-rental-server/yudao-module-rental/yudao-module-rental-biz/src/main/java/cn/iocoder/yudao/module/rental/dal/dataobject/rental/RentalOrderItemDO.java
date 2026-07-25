package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Converted source product/SKU line for a rental order.
 */
@TableName("rental_order_item")
@KeySequence("rental_order_item_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderItemDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long rentalOrderId;
    private String equipmentModelCode;
    private String sourceProductId;
    private String sourceSkuId;
    private Integer quantity;
    private Long rentAmount;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;

}
