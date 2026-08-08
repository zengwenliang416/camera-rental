package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 待分配租赁订单")
@Data
public class RentalPendingAllocationOrderRespVO {

    private Long id;
    private String orderNo;
    private String sourceType;
    private String sourceOrderId;
    private String status;
    private Long rentAmount;
    private Long refundAmount;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;

    @Schema(description = "订单内待分配明细的所需总数")
    private Integer requiredQuantity;

    @Schema(description = "订单内待分配明细的已分配总数")
    private Integer assignedQuantity;

    @Schema(description = "订单内待分配明细的剩余总数")
    private Integer remainingQuantity;

    private List<RentalPendingAllocationItemRespVO> items;
}
