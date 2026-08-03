package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Schema(description = "管理后台 - 租赁经营报表查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RentalReportQueryReqVO extends PageParam {

    private static final long MAX_RANGE_DAYS = 366;

    @Schema(description = "统计开始日（含）")
    @NotNull(message = "统计开始日不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "统计结束日（含）")
    @NotNull(message = "统计结束日不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @AssertTrue(message = "统计结束日不能早于开始日，且区间不能超过 366 天")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return false;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1 <= MAX_RANGE_DAYS;
    }

}
