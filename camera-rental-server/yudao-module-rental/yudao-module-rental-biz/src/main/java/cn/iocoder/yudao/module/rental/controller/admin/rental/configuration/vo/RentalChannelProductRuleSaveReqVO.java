package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RentalChannelProductRuleSaveReqVO {

    private Long id;
    @NotNull
    private Long shopId;
    @NotBlank
    private String xianyuItemId;
    @NotBlank
    private String handlingPolicy;
    private String mappingMode;
    private Long singleDeviceModelId;
    @NotNull
    private Boolean enabled;
    private String ruleNote;
    private Integer lockVersion;
    @Valid
    private List<RentalChannelProductRuleSkuReqVO> skuMappings;

}
