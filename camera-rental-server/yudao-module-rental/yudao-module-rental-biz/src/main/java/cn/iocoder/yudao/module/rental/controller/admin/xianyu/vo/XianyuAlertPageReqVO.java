package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 闲鱼运营告警分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class XianyuAlertPageReqVO extends PageParam {

    @Schema(description = "店铺编号", example = "1")
    private Long shopId;

    @Schema(description = "告警类型", example = "SHOP_AUTH_INVALID")
    private String alertType;

    @Schema(description = "告警状态", example = "OPEN")
    private String status;

    @Schema(description = "告警级别", example = "WARNING")
    private String severity;

}
