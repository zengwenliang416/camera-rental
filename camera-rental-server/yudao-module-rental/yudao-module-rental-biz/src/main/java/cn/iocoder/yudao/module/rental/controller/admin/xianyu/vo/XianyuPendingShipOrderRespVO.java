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
    private String receiverName;
    private String receiverMobile;
    private String receiverAddress;
    private String sellerRemark;
    private String xianyuItemId;
    private Long rentalOrderId;
    private String conversionStatus;
    private String preparationStatus;
    private String preparationReasonCode;
    private LocalDateTime orderTime;
    private LocalDateTime sourceUpdatedAt;

}
