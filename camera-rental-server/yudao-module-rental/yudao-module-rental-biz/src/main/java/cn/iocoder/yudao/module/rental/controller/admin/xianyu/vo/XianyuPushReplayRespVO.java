package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 闲管家推送事件重放 Response VO")
@Data
public class XianyuPushReplayRespVO {

    @Schema(description = "推送事件编号")
    private Long eventId;

    @Schema(description = "重放结果", example = "QUEUED")
    private String status;

    @Schema(description = "安全错误码")
    private String safeErrorCode;

    @Schema(description = "脱敏消息")
    private String message;

}
