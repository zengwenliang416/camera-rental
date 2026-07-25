package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 租赁排期分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RentalSchedulePageReqVO extends PageParam {

    private Long deviceId;
    private Long rentalOrderId;
    private String status;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate occupyStartDate;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate occupyEndDateExclusive;

}
