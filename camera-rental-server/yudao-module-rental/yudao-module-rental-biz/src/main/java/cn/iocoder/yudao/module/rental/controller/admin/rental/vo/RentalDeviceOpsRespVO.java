package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备出库/回仓结果")
@Data
public class RentalDeviceOpsRespVO {

    private Long deviceId;
    private String deviceNo;
    private String deviceStatus;
    private Long assignmentId;
    private String assignmentStatus;

}
