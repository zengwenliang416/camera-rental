package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 撤销设备分配")
@Data
public class RentalDeviceUnassignReqVO {

    @Schema(description = "分配记录 ID")
    @NotNull
    private Long assignmentId;

}
