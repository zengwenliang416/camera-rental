package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 设备分配候选")
@Data
public class RentalDeviceCandidateRespVO {

    private Long id;
    private String deviceNo;
    private String serialNumber;
    private String equipmentModelCode;
    private String status;
    private Boolean enabled;
    private Boolean eligible;
    private List<String> reasonCodes;
    private LocalDate nextAvailableDate;
    private List<RentalScheduleSegmentRespVO> neighboringSchedules;
    private List<RentalScheduleLockRespVO> activeLocks;
    private List<RentalScheduleDeliveryRespVO> logistics;
}
