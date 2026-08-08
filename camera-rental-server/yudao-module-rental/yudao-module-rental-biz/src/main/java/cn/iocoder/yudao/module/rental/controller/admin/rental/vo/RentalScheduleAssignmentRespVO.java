package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 设备分配及其占用排期")
@Data
public class RentalScheduleAssignmentRespVO {

    private Long id;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private Long deviceId;
    private String deviceNo;
    private String serialNumber;
    private String deviceStatus;
    private Boolean deviceEnabled;
    private String status;
    private Long scheduleId;
    private String scheduleStatus;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
    private LocalDateTime assignedAt;
}
