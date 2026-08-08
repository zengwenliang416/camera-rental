package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 设备排期详情")
@Data
public class RentalDeviceScheduleDetailRespVO {

    private Long id;
    private String deviceNo;
    private String serialNumber;
    private String equipmentModelCode;
    private String status;
    private Boolean enabled;
    private String inspectionState;
    private String maintenanceState;
    private LocalDate expectedReleaseDate;
    private List<String> reasonCodes;
    private RentalScheduleAssignmentRespVO currentAssignment;
    private List<RentalScheduleSegmentRespVO> schedules;
    private List<RentalScheduleDeliveryRespVO> deliveries;
    private List<RentalScheduleLockRespVO> activeLocks;
}
