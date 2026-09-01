package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import lombok.Data;

@Data
public class RentalChannelProductRuleImpactRespVO {

    private Long scannedCount;
    private Long withoutInternalOrderCount;
    private Long mutableInternalOrderCount;
    private Long protectedOrderCount;
    private Long reviewRequiredCount;

}
