package cn.iocoder.yudao.module.rental.dal.dataobject.logistics;

import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "rental_delivery_callback_inbox", autoResultMap = true)
@KeySequence("rental_delivery_callback_inbox_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeliveryCallbackInboxDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String providerCode;
    private Long deliveryId;
    private String providerTaskId;
    private String payloadHash;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String callbackParams;
    private String processingStatus;
    private String processingToken;
    private LocalDateTime leaseUntil;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
}
