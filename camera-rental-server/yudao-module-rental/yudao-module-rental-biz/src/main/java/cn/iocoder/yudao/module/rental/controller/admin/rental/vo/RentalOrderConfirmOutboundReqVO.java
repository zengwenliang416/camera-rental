package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 线下订单无运单出库确认")
@Data
public class RentalOrderConfirmOutboundReqVO {

    @Schema(description = "租赁订单 ID")
    @NotNull
    private Long orderId;

}
