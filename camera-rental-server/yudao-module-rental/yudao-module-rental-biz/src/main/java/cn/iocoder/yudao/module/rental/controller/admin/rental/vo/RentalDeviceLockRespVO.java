package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RentalDeviceLockRespVO {

    private Long id;
    private Long deviceId;
    private String lockType;
    private String reason;
    private Long rentalOrderId;
    private Long rentalOrderItemId;
    private String sourceType;
    private LocalDateTime startTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime releasedAt;
    private Long releasedBy;
    private String releaseReason;
    private String status;
}
