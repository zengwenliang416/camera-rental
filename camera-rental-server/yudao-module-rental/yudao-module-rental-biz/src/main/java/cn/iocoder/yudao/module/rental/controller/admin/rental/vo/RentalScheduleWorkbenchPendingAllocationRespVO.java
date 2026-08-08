package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 设备排期工作台待分配订单明细")
@Data
public class RentalScheduleWorkbenchPendingAllocationRespVO {

    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String orderNo;
    private String orderStatus;
    private String equipmentModelCode;
    private Integer requiredQuantity;
    private Integer assignedQuantity;
    private Integer remainingQuantity;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
}
