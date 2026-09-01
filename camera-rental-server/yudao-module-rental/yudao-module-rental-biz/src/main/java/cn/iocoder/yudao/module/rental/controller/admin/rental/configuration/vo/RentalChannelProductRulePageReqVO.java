package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RentalChannelProductRulePageReqVO extends PageParam {

    private Long shopId;
    private String handlingPolicy;
    private Boolean enabled;
    private String keyword;

}
