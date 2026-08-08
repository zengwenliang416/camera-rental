package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 设备排期工作台占用条带")
@Data
public class RentalScheduleWorkbenchSegmentRespVO {

    private Long scheduleId;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private Long assignmentId;
    private String orderNo;
    private String segmentType;
    private String scheduleType;
    private String status;
    private String logisticsStatus;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
    private LocalDate displayStartDate;
    private LocalDate displayEndDateExclusive;
    private Boolean leftContinuation;
    private Boolean rightContinuation;
}
