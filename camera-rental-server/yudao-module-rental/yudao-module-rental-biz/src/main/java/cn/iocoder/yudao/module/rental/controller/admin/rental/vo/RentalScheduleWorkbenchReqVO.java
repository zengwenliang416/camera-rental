package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 设备排期工作台查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RentalScheduleWorkbenchReqVO extends PageParam {

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @Schema(description = "窗口开始日期，包含")
    private LocalDate fromDate;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @Schema(description = "窗口结束日期，不包含")
    private LocalDate toDateExclusive;

    @Schema(description = "视图模式，支持 14D、30D、90D")
    private String viewMode;

    @Schema(description = "设备编号、旧编号、序列号或型号关键词")
    private String keyword;

    @Schema(description = "设备型号编码")
    private String equipmentModelCode;

    @Schema(description = "设备状态")
    private String deviceStatus;

    @Schema(description = "本地物流状态")
    private String logisticsStatus;
}
