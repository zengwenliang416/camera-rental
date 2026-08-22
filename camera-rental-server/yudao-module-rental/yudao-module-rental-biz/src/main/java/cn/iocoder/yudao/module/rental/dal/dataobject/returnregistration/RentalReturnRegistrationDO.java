package cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("rental_return_registration")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class RentalReturnRegistrationDO extends TenantBaseDO {
    @TableId
    private Long id;
    private String formNo;
    private Long rentalOrderId;
    private Long channelOrderId;
    private String externalOrderNo;
    private String tokenHash;
    private String status;
    private String returnMethod;
    private String carrierCode;
    private String carrierName;
    private String waybillNo;
    private String normalizedWaybillNo;
    private LocalDate shippedDate;
    private String issueDescription;
    private Long deliveryId;
    private String idempotencyKey;
    private LocalDateTime expiresAt;
    private LocalDateTime openedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewerId;
    private String reviewNote;
}
