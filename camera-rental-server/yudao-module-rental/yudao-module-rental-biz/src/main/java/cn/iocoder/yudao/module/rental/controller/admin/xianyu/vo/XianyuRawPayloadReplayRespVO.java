package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 闲管家原始载荷重放 Response VO")
@Data
public class XianyuRawPayloadReplayRespVO {

    @Schema(description = "原始载荷编号")
    private Long rawPayloadId;

    @Schema(description = "受影响的渠道订单编号")
    private Long orderId;

    @Schema(description = "重放结果", example = "REPLAYED")
    private String status;

    @Schema(description = "安全错误码")
    private String safeErrorCode;

    @Schema(description = "脱敏消息")
    private String message;

}
