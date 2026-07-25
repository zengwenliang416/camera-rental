package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 闲鱼同步运行历史 Response VO")
@Data
public class XianyuSyncRunRespVO {

    @Schema(description = "运行编号", example = "1")
    private Long id;

    @Schema(description = "店铺编号", example = "1")
    private Long shopId;

    @Schema(description = "资源类型", example = "ORDER")
    private String resourceType;

    @Schema(description = "触发方式", example = "MANUAL")
    private String triggerType;

    @Schema(description = "运行状态", example = "SUCCEEDED")
    private String status;

    @Schema(description = "同步窗口开始时间")
    private LocalDateTime windowStart;

    @Schema(description = "同步窗口结束时间")
    private LocalDateTime windowEnd;

    @Schema(description = "接收记录数", example = "10")
    private Integer receivedCount;

    @Schema(description = "去重记录数", example = "2")
    private Integer deduplicatedCount;

    @Schema(description = "成功记录数", example = "8")
    private Integer succeededCount;

    @Schema(description = "进入复核记录数", example = "1")
    private Integer reviewRequiredCount;

    @Schema(description = "失败记录数", example = "1")
    private Integer failedCount;

    @Schema(description = "最近错误码", example = "XIANYU_ORDER_SYNC_FAILED")
    private String lastErrorCode;

    @Schema(description = "最近错误信息（已脱敏）", example = "timeout")
    private String lastErrorMessage;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime finishedAt;

}
