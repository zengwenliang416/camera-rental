package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import lombok.Data;

@Data
public class RentalChannelProductRuleSaveRespVO {

    private Long ruleId;
    private Integer lockVersion;
    private RentalChannelProductRuleImpactRespVO impact;
    private Long reconciliationRunId;

}
