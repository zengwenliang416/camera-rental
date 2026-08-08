package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 有效设备占用排期")
@Data
public class RentalScheduleSegmentRespVO {

    private Long id;
    private Long deviceId;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String orderNo;
    private String scheduleType;
    private String status;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
}
