package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 本地物流快照摘要")
@Data
public class RentalScheduleDeliveryRespVO {

    private Long id;
    private Long rentalOrderId;
    private String direction;
    private Integer packageSeq;
    private String sourceCarrierName;
    private String trackingStatus;
    private String mappingStatus;
    private String subscribeStatus;
    private String queryStatus;
    private LocalDateTime latestEventTime;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime estimatedDeliveryAt;
    private Boolean stale;
    private List<Long> deviceIds;
}
