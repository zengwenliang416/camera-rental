package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 设备出库")
@Data
public class RentalDeviceDispatchReqVO {

    @NotNull
    private Long deviceId;

    /** Optional; when null, use the active ASSIGNED assignment on this device. */
    private Long assignmentId;

}
