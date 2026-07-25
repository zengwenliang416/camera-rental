package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 闲鱼渠道订单摘要")
@Data
public class XianyuOrderRespVO {

    private Long id;
    private Long shopId;
    private String externalOrderId;
    private String externalProductId;
    private String externalSkuId;
    private String orderStatus;
    /** 单位：分 */
    private Long payAmount;
    private String currency;
    private String sellerRemark;
    private String remarkParseStatus;
    private String conversionStatus;
    private Long rentalOrderId;
    private LocalDateTime sourceCreatedAt;
    private LocalDateTime sourceUpdatedAt;

    private Integer orderType;
    private LocalDateTime orderTime;
    private Long totalAmount;
    private LocalDateTime payTime;
    private Integer refundStatus;
    private Long refundAmount;
    private LocalDateTime refundTime;
    private String expressCode;
    private String expressName;
    private Long expressFee;
    private Integer consignType;
    private LocalDateTime consignTime;
    private LocalDateTime confirmTime;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private String sellerName;
    private String goodsTitle;
    private Integer goodsQuantity;
    private Long goodsPrice;
    private Long xybSellerAmount;
    private Boolean taxIncluded;
    private Integer idleBizType;
    private Integer pinGroupStatus;

}
