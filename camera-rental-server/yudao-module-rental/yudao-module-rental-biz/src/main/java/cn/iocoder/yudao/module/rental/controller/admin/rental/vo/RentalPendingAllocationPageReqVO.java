package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 待分配租赁订单分页查询")
@Data
@EqualsAndHashCode(callSuper = true)
public class RentalPendingAllocationPageReqVO extends PageParam {

    @Schema(description = "内部订单号，支持模糊匹配")
    private String orderNo;

    @Schema(description = "设备型号编码")
    private String equipmentModelCode;
}
