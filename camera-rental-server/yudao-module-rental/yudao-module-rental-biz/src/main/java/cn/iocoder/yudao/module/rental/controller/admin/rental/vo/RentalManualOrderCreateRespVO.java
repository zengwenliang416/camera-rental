package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 线下租赁订单手动创建结果")
@Data
public class RentalManualOrderCreateRespVO {

    @Schema(description = "租赁订单 ID")
    private Long id;

    @Schema(description = "租赁订单号")
    private String orderNo;

}
