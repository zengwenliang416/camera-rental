package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RentalDeviceLockCreateReqVO {

    @NotNull
    private Long deviceId;

    @NotBlank
    private String lockType;

    @NotBlank
    @Size(max = 512)
    private String reason;

    private Long rentalOrderId;
    private Long rentalOrderItemId;

    private LocalDateTime plannedEndTime;
}
