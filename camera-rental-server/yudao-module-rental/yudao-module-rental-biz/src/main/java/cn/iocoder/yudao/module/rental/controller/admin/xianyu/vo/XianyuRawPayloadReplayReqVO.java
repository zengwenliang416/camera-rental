package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 闲管家原始载荷重放 Request VO")
@Data
public class XianyuRawPayloadReplayReqVO {

    @Schema(description = "原始载荷编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "原始载荷编号不能为空")
    private Long rawPayloadId;

}
