package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 恢复历史订单补建任务 Request VO")
@Data
public class RentalHistoricalBackfillResumeReqVO {

    @Schema(description = "任务编号", example = "1")
    @NotNull
    @Min(1)
    private Long runId;

    @Schema(description = "本次请求最多执行批次数", example = "10")
    @NotNull
    @Min(1)
    @Max(100)
    private Integer maxBatches;

    @Schema(description = "真实执行确认串")
    @Size(max = 64)
    private String confirmation;

}
