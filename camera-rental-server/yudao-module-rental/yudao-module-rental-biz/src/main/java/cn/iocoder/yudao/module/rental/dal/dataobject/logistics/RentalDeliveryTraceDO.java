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

@TableName("rental_delivery_trace")
@KeySequence("rental_delivery_trace_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeliveryTraceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deliveryId;
    private Integer snapshotVersion;
    private String snapshotHash;
    private Integer eventSeq;
    private String eventFingerprint;
    private LocalDateTime businessTime;
    private String rawTime;
    private String trackingStatus;
    private String providerStatus;
    private String traceText;
    private String location;
    private String eventSource;
    private Long inboxId;
}
