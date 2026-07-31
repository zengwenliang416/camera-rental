package cn.iocoder.yudao.module.rental.dal.dataobject.logistics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("rental_delivery_outbox")
@KeySequence("rental_delivery_outbox_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeliveryOutboxDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deliveryId;
    private String eventType;
    private String dedupeKey;
    private String safeMetadata;
    private String processingStatus;
    private String processingToken;
    private LocalDateTime leaseUntil;
    private Integer retryCount;
    private LocalDateTime nextAttemptAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime scheduledAt;
    private LocalDateTime processedAt;
}
