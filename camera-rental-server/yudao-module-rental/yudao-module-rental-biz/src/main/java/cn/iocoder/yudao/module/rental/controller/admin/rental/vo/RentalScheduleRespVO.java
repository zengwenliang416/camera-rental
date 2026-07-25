package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 租赁排期")
@Data
public class RentalScheduleRespVO {

    private Long id;
    private Long deviceId;
    private String deviceNo;
    private String equipmentModelCode;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String scheduleType;
    private String status;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;

}
