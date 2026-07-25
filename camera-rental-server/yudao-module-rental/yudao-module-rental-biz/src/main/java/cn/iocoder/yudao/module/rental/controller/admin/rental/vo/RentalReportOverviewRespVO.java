package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 租赁经营报表总览")
@Data
public class RentalReportOverviewRespVO {

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer orderCount;
    private Long rentAmountFen;
    private Long refundAmountFen;
    private Integer deviceCount;
    private Long totalDeviceDays;
    private Long occupiedDeviceDays;
    private Long idleDeviceDays;
    private Integer utilizationBasisPoints;
    private Long assignedIncomeFen;
    private String currency;
    private List<RentalReportSourceRespVO> sources;

}
