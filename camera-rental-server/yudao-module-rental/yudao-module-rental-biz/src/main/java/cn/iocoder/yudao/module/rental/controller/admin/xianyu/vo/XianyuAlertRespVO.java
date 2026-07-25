package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 闲鱼运营告警 Response VO")
@Data
public class XianyuAlertRespVO {

    @Schema(description = "告警编号", example = "1")
    private Long id;

    @Schema(description = "店铺编号", example = "1")
    private Long shopId;

    @Schema(description = "告警类型", example = "SHOP_AUTH_INVALID")
    private String alertType;

    @Schema(description = "告警级别", example = "WARNING")
    private String severity;

    @Schema(description = "告警状态", example = "OPEN")
    private String status;

    @Schema(description = "来源标识（默认脱敏）")
    private String sourceIdentifier;

    @Schema(description = "告警消息（默认脱敏）")
    private String message;

    @Schema(description = "首次发现时间")
    private LocalDateTime firstSeenAt;

    @Schema(description = "最近发现时间")
    private LocalDateTime lastSeenAt;

    @Schema(description = "解决时间")
    private LocalDateTime resolvedAt;

}
