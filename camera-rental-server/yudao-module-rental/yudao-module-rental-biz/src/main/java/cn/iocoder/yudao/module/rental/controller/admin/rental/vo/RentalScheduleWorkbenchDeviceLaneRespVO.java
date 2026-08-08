package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 设备排期工作台设备行")
@Data
public class RentalScheduleWorkbenchDeviceLaneRespVO {

    private Long deviceId;
    private String deviceNo;
    private String legacyDeviceNo;
    private String serialNumber;
    private String equipmentModelCode;
    private String warehouseCode;
    private Boolean enabled;
    private String deviceStatus;
    private String logisticsStatus;
    private Boolean occupied;
    private LocalDate expectedReleaseDate;
    private List<RentalScheduleWorkbenchSegmentRespVO> segments = List.of();
}
