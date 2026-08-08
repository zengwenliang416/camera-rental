package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 设备排期工作台窗口")
@Data
public class RentalScheduleWorkbenchWindowRespVO {

    private LocalDate fromDate;
    private LocalDate toDateExclusive;
    private String viewMode;
    private Integer dayCount;
    private String timezone;
}
