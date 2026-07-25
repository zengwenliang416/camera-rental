package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 闲鱼同步运行历史分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class XianyuSyncRunPageReqVO extends PageParam {

    @Schema(description = "店铺编号", example = "1")
    private Long shopId;

    @Schema(description = "资源类型", example = "ORDER")
    private String resourceType;

    @Schema(description = "运行状态", example = "SUCCEEDED")
    private String status;

    @Schema(description = "触发方式", example = "MANUAL")
    private String triggerType;

}
