package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 活动设备锁定摘要")
@Data
public class RentalScheduleLockRespVO {

    private Long id;
    private Long deviceId;
    private String lockType;
    private String reason;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String sourceType;
    private LocalDateTime startTime;
    private LocalDateTime plannedEndTime;
    private String status;
}
