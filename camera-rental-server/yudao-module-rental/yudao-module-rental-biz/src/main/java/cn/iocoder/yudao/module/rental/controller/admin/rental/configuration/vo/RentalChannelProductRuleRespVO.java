package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import lombok.Data;

import java.util.List;

@Data
public class RentalChannelProductRuleRespVO {

    private Long id;
    private Long shopId;
    private String xianyuItemId;
    private String xgjProductId;
    private String productTitleSnapshot;
    private String handlingPolicy;
    private String mappingMode;
    private Long singleDeviceModelId;
    private Boolean enabled;
    private String ruleNote;
    private Integer lockVersion;
    private List<RentalChannelProductSkuRespVO> skuMappings;

}
