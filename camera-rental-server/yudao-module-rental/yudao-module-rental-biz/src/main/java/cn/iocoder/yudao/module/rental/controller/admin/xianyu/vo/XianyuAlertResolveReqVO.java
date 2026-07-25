package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 解决闲鱼运营告警 Request VO")
@Data
public class XianyuAlertResolveReqVO {

    @Schema(description = "告警编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "告警编号不能为空")
    private Long id;

}
