package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 排期工作台订单明细")
@Data
public class RentalScheduleOrderItemRespVO {

    private Long id;
    private Long rentalOrderId;
    private String equipmentModelCode;
    private Integer requiredQuantity;
    private Integer assignedQuantity;
    private Integer remainingQuantity;
    private Long rentAmount;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
    private List<RentalScheduleAssignmentRespVO> assignments;
}
