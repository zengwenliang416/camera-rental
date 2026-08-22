package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 取消租赁订单")
@Data
public class RentalOrderCancelReqVO {

    @Schema(description = "租赁订单 ID")
    @NotNull
    private Long orderId;

    @Schema(description = "取消原因")
    @Size(max = 512)
    private String reason;

}
