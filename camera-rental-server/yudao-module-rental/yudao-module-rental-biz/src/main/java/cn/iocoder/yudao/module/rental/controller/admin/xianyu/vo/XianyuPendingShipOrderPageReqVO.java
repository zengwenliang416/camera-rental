package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 闲鱼待发货订单搜索 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class XianyuPendingShipOrderPageReqVO extends PageParam {

    private Long shopId;

    @Schema(description = "订单号、收货人姓名或完整收货手机号")
    private String keyword;

}
