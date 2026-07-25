package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 闲管家推送事件重放 Request VO")
@Data
public class XianyuPushReplayReqVO {

    @Schema(description = "推送事件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "推送事件编号不能为空")
    private Long eventId;

}
