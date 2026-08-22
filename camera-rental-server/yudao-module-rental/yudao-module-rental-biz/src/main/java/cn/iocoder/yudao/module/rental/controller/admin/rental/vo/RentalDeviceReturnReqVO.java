package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 设备回仓")
@Data
public class RentalDeviceReturnReqVO {

    @Schema(description = "设备 ID；与 deviceNo 二选一")
    private Long deviceId;

    @Schema(description = "设备编号（设备二维码缺失时人工录入）")
    @Size(max = 64)
    private String deviceNo;

    /**
     * true = 检测通过 → AVAILABLE；false = 检测不通过 → MAINTENANCE。
     * null defaults to true.
     */
    private Boolean inspectPassed;

    @Schema(description = "回仓检测备注（落库到分配记录）")
    @Size(max = 512)
    private String note;

}
