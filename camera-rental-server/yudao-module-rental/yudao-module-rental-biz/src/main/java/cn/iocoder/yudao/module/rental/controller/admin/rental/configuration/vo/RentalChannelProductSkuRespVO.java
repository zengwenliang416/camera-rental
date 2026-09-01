package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import lombok.Data;

@Data
public class RentalChannelProductSkuRespVO {

    private Long productSkuId;
    private String xgjSkuId;
    private String xianyuSkuId;
    private String skuName;
    private String status;
    private Long deviceModelId;
    private Boolean mappingEnabled;

}
