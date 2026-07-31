package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 本地物流详情")
@Data
public class RentalDeliveryTrackingDetailRespVO {

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
    private String latestLocation;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime latestEventTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedDeliveryAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextQueryAllowedAt;
    private Boolean stale;
    private List<RentalDeliveryTrackingDeviceRespVO> devices = new ArrayList<>();
    private List<RentalDeliveryTrackingTraceRespVO> traces = new ArrayList<>();
    private List<RentalDeliveryTrackingRiskRespVO> risks = new ArrayList<>();
}
