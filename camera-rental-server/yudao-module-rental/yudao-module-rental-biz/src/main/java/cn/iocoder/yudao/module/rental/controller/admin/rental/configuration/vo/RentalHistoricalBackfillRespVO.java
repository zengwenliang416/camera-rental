package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import cn.iocoder.yudao.module.rental.service.reconciliation.RentalHistoricalBackfillRunResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 历史订单补建任务 Response VO")
@Data
@Builder
public class RentalHistoricalBackfillRespVO {

    private Long runId;
    private Boolean dryRun;
    private String status;
    private Long startAfterId;
    private Long endIdInclusive;
    private Long cursorAfterId;
    private Integer batchSize;
    private Integer resumeCount;
    private Integer scannedCount;
    private Integer skippedCount;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer unchangedCount;
    private Integer conflictCount;
    private Integer failedCount;
    private Integer reviewRequiredCount;
    private Long lastFailedOrderId;
    private String lastErrorCode;

    public static RentalHistoricalBackfillRespVO from(
            RentalHistoricalBackfillRunResult result) {
        return RentalHistoricalBackfillRespVO.builder()
                .runId(result.runId())
                .dryRun(result.dryRun())
                .status(result.status())
                .startAfterId(result.startAfterId())
                .endIdInclusive(result.endIdInclusive())
                .cursorAfterId(result.cursorAfterId())
                .batchSize(result.batchSize())
                .resumeCount(result.resumeCount())
                .scannedCount(result.scannedCount())
                .skippedCount(result.skippedCount())
                .createdCount(result.createdCount())
                .updatedCount(result.updatedCount())
                .unchangedCount(result.unchangedCount())
                .conflictCount(result.conflictCount())
                .failedCount(result.failedCount())
                .reviewRequiredCount(result.reviewRequiredCount())
                .lastFailedOrderId(result.lastFailedOrderId())
                .lastErrorCode(result.lastErrorCode())
                .build();
    }

}
