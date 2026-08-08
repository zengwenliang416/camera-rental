package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 租赁订单排期详情")
@Data
public class RentalOrderScheduleDetailRespVO {

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
    private Integer requiredQuantity;
    private Integer assignedQuantity;
    private Integer remainingQuantity;
    private List<String> riskCodes;
    private List<RentalScheduleOrderItemRespVO> items;
    private List<RentalScheduleDeliveryRespVO> deliveries;
}
