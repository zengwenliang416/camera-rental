package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 设备分配")
@Data
public class RentalDeviceAssignReqVO {

    @NotNull
    private Long rentalOrderItemId;
    @NotNull
    private Long deviceId;
    @NotNull
    private LocalDate occupyStartDate;
    @NotNull
    private LocalDate occupyEndDateExclusive;
    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    @AssertTrue(message = "设备占用结束日必须晚于开始日")
    public boolean isOccupyPeriodValid() {
        return occupyStartDate == null || occupyEndDateExclusive == null
                || occupyStartDate.isBefore(occupyEndDateExclusive);
    }

}
