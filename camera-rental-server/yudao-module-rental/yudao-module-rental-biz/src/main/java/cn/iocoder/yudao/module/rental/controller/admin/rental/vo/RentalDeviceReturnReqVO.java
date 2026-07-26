package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 设备回仓")
@Data
public class RentalDeviceReturnReqVO {

    @NotNull
    private Long deviceId;

    /**
     * true = 检测通过 → AVAILABLE；false = 检测不通过 → MAINTENANCE。
     * null defaults to true.
     */
    private Boolean inspectPassed;

    private String note;

}
