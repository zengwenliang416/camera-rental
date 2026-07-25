package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 租赁设备利用率与收入报表")
@Data
public class RentalDevicePerformanceReportRespVO {

    private Long deviceId;
    private String deviceNo;
    private String equipmentModelCode;
    private String status;
    private Long totalDays;
    private Long occupiedDays;
    private Long idleDays;
    private Integer utilizationBasisPoints;
    private Integer scheduleCount;
    private Integer assignmentCount;
    private Long assignedIncomeFen;
    private Long latestRentalOrderId;

}
