package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 设备排期工作台异常摘要")
@Data
public class RentalScheduleWorkbenchExceptionRespVO {

    private String code;
    private String severity;
    private String message;
    private String nextAction;
    private Long deviceId;
    private String deviceNo;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String sourceType;
    private String sourceId;
    private LocalDate expectedReleaseDate;
}
