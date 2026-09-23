package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public class RentalStaffOrderRespVO extends RentalPendingAllocationOrderRespVO {
    private Long channelOrderId;
    private String preparationStatus;
    private String preparationReasonCode;
    private String conversionStatus;
    @ToString.Exclude private String receiverName;
    @ToString.Exclude private String receiverMobile;
    @ToString.Exclude private String receiverAddress;
    /** UNSHIPPED / PARTIAL / SHIPPED / CANCELED */
    private String shippingStatus;
}
