package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 待分配租赁订单明细")
@Data
public class RentalPendingAllocationItemRespVO {

    private Long id;
    private Long rentalOrderId;
    private String equipmentModelCode;

    @Schema(description = "订单明细需要的设备数量")
    private Integer requiredQuantity;

    @Schema(description = "订单明细已经分配的设备数量")
    private Integer assignedQuantity;

    @Schema(description = "订单明细仍需分配的设备数量")
    private Integer remainingQuantity;

    private Long rentAmount;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
}
