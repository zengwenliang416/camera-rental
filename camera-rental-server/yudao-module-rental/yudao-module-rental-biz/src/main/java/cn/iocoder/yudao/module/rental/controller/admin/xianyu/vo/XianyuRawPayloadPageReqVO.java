package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 闲管家受限原始载荷分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class XianyuRawPayloadPageReqVO extends PageParam {

    @Schema(description = "来源类型", example = "ORDER_DETAIL")
    private String sourceType;

    @Schema(description = "来源标识（精确查询）", example = "3364202298717566229")
    private String sourceIdentifier;

}
