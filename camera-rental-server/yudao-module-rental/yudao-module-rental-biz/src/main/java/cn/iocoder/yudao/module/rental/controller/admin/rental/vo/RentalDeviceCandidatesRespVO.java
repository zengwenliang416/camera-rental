package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 订单明细设备候选结果")
@Data
public class RentalDeviceCandidatesRespVO {

    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String orderNo;
    private String equipmentModelCode;
    private Integer requiredQuantity;
    private Integer assignedQuantity;
    private Integer remainingQuantity;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
    private List<String> reasonCodes;
    private List<RentalDeviceCandidateRespVO> candidates;
}
