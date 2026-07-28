package cn.iocoder.yudao.module.rental.dal.dataobject.xianyu;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Channel order: queryable columns + full detail_json (all order-detail fields).
 */
@TableName("xianyu_order")
@KeySequence("xianyu_order_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuOrderDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String externalOrderId;
    private String externalProductId;
    private String externalSkuId;
    private String orderStatus;
    private Long payAmount;
    private String currency;
    private String sellerRemark;
    private String remarkParseVersion;
    private String remarkParseStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate billableStartDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate billableEndDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate shipDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate receiveDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate returnDate;
    private String rentalPeriodStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rentalPeriodReasonCode;
    private LocalDateTime sourceCreatedAt;
    private LocalDateTime sourceUpdatedAt;
    private Long rawPayloadId;
    private String conversionStatus;
    private Long rentalOrderId;

    /** Full order-detail {@code data} JSON from XianGuanJia. */
    private String detailJson;
    private String receiverName;
    private String receiverMobile;
    private String receiverAddress;
    private Integer orderType;
    private LocalDateTime orderTime;
    private Long totalAmount;
    private String payNo;
    private LocalDateTime payTime;
    private Integer refundStatus;
    private Long refundAmount;
    private LocalDateTime refundTime;
    private String waybillNo;
    private String expressCode;
    private String expressName;
    private Long expressFee;
    private Integer consignType;
    private LocalDateTime consignTime;
    private LocalDateTime confirmTime;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private String buyerNick;
    private String sellerName;
    private String goodsTitle;
    private Integer goodsQuantity;
    private Long goodsPrice;
    private String goodsJson;
    private Long xybSellerAmount;
    @TableField("is_tax_included")
    private Boolean taxIncluded;
    private Integer idleBizType;
    private Integer pinGroupStatus;

}
