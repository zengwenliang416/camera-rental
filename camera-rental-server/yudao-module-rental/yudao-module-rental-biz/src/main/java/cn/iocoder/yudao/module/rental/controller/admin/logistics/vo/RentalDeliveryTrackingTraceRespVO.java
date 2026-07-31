package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 当前物流快照轨迹")
@Data
public class RentalDeliveryTrackingTraceRespVO {

    private Integer eventSeq;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime businessTime;
    private String trackingStatus;
    private String traceText;
    private String location;
}
