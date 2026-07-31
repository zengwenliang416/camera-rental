package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 本地物流包裹摘要")
@Data
public class RentalDeliveryTrackingPackageSummaryRespVO {

    private Long deliveryId;
    private Long rentalOrderId;
    private String direction;
    private Integer packageSeq;
    private String carrierName;
    private String maskedWaybillNo;
    private String trackingStatus;
    private String mappingStatus;
    private String subscribeStatus;
    private String queryStatus;
    private String latestTraceText;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime latestEventTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedDeliveryAt;
    private Boolean stale;
    private RentalDeliveryTrackingRiskRespVO risk;
}
