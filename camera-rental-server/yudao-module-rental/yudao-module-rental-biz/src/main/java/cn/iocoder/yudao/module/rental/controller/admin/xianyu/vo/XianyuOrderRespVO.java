package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 闲鱼渠道订单摘要")
@Data
public class XianyuOrderRespVO {

    private Long id;
    private Long shopId;
    /** Full channel order no. — not redacted; used for ops lookup. */
    private String externalOrderId;
    private String xgjProductId;
    private String xianyuItemId;
    private String xgjSkuId;
    private String xianyuSkuId;
    private String orderStatus;
    /** 单位：分 */
    private Long payAmount;
    private String currency;
    private String sellerRemark;
    /** Complete persisted receiver snapshot for authorized management users. */
    private String receiverName;
    private String receiverMobile;
    private String receiverAddress;
    private String buyerNick;
    private String remarkParseVersion;
    private String remarkParseStatus;
    private String remarkParseSource;
    private BigDecimal remarkParseConfidence;
    private String remarkParseModel;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate shipDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate billableStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate billableEndDate;
    private String rentalPeriodStatus;
    private String rentalPeriodReasonCode;
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
    private String waybillNo;
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

    private Long rentalOrderItemId;
    private String equipmentModelCode;
    private Integer rentalQuantity;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate occupyStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate occupyEndDateExclusive;
    private List<Long> assignedDeviceIds;

}
