package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalChannelProductRuleSkuReqVO {

    @NotNull
    private Long productSkuId;
    @NotNull
    private Long deviceModelId;
    @NotNull
    private Boolean enabled;

}
