package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelReconciliationRunDO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RentalChannelReconciliationRunRespVO {

    private Long runId;
    private Long productRuleId;
    private Long shopId;
    private String xianyuItemId;
    private String triggerType;
    private String status;
    private Integer scannedCount;
    private Integer skippedCount;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer unchangedCount;
    private Integer conflictCount;
    private Integer failedCount;
    private Integer reviewRequiredCount;
    private String lastErrorCode;

    public static RentalChannelReconciliationRunRespVO from(
            RentalChannelReconciliationRunDO run) {
        return RentalChannelReconciliationRunRespVO.builder()
                .runId(run.getId())
                .productRuleId(run.getProductRuleId())
                .shopId(run.getShopId())
                .xianyuItemId(run.getXianyuItemId())
                .triggerType(run.getTriggerType())
                .status(run.getStatus())
                .scannedCount(run.getScannedCount())
                .skippedCount(run.getSkippedCount())
                .createdCount(run.getCreatedCount())
                .updatedCount(run.getUpdatedCount())
                .unchangedCount(run.getUnchangedCount())
                .conflictCount(run.getConflictCount())
                .failedCount(run.getFailedCount())
                .reviewRequiredCount(run.getReviewRequiredCount())
                .lastErrorCode(run.getLastErrorCode())
                .build();
    }

}
