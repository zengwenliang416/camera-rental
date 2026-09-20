package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 租赁订单排期详情")
@Data
public class RentalOrderScheduleDetailRespVO {

    private Long id;
    private String orderNo;
    private String externalOrderNo;
    @Schema(description = "渠道商品标题，闲鱼单取 listing 标题")
    private String goodsTitle;
    @Schema(description = "闲鱼买家昵称")
    private String buyerNick;
    @Schema(description = "收货人姓名")
    private String receiverName;
    @Schema(description = "收货人手机")
    private String receiverMobile;
    @Schema(description = "收货地址")
    private String receiverAddress;
    private String sourceType;
    private String sourceOrderId;
    private String status;
    private Long rentAmount;
    private Long refundAmount;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate occupyStartDate;
    private LocalDate occupyEndDateExclusive;
    private Integer requiredQuantity;
    private Integer assignedQuantity;
    private Integer remainingQuantity;
    private List<String> riskCodes;
    private List<RentalScheduleOrderItemRespVO> items;
    private List<RentalScheduleDeliveryRespVO> deliveries;
}
