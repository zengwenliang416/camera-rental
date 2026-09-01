package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 创建历史订单补建任务 Request VO")
@Data
public class RentalHistoricalBackfillCreateReqVO {

    @Schema(description = "起始订单主键，不包含该值", example = "0")
    @NotNull
    @Min(0)
    private Long startAfterId;

    @Schema(description = "结束订单主键，包含该值", example = "100000")
    @NotNull
    @Min(1)
    private Long endIdInclusive;

    @Schema(description = "单批订单数", example = "100")
    @NotNull
    @Min(1)
    @Max(500)
    private Integer batchSize;

    @Schema(description = "本次请求最多执行批次数", example = "10")
    @NotNull
    @Min(1)
    @Max(100)
    private Integer maxBatches;

    @Schema(description = "是否只预演", example = "true")
    @NotNull
    private Boolean dryRun;

    @Schema(description = "真实执行确认串")
    @Size(max = 64)
    private String confirmation;

    @AssertTrue(message = "结束订单主键必须大于起始订单主键")
    public boolean isIdRangeValid() {
        return startAfterId == null || endIdInclusive == null
                || endIdInclusive > startAfterId;
    }

}
