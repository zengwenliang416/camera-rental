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

@TableName(value = "rental_delivery", autoResultMap = true)
@KeySequence("rental_delivery_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeliveryDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long rentalOrderId;
    private Long channelOrderId;
    private String direction;
    private Integer packageSeq;
    private String sourceType;
    private String sourceIdentifier;
    private String sourceCarrierCode;
    private String sourceCarrierName;
    private String canonicalCarrierCode;
    private String providerCode;
    private String providerCarrierCode;
    private Long providerCredentialId;
    @ToString.Exclude
    private String waybillNo;
    @ToString.Exclude
    private String normalizedWaybillNo;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String trackingPhone;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String callbackToken;
    @ToString.Exclude
    private String callbackTokenHash;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String callbackSalt;
    private String lifecycleStatus;
    private String mappingStatus;
    private String subscribeStatus;
    private String queryStatus;
    private String trackingStatus;
    private Integer trackingVersion;
    private String currentSnapshotHash;
    private LocalDateTime latestEventTime;
    private String latestTraceText;
    private String latestLocation;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastCallbackAt;
    private LocalDateTime nextQueryAllowedAt;
    private String subscribeMonth;
    private Integer subscribeCount;
    private LocalDateTime nextSubscribeAllowedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
}
