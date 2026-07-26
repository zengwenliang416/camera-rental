package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 闲鱼待发货订单候选")
@Data
public class XianyuPendingShipOrderRespVO {

    private Long id;
    private Long shopId;
    private String externalOrderId;
    private String orderStatus;
    private String goodsTitle;
    private Integer goodsQuantity;
    private Long payAmount;
    private String buyerNick;
    private Long rentalOrderId;
    private String conversionStatus;
    private LocalDateTime orderTime;
    private LocalDateTime sourceUpdatedAt;

}
