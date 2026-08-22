package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - 按发货日金额汇总 Response VO")
@Data
public class RentalShipDateSummaryRespVO {

    @Schema(description = "发货日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Schema(description = "当日发货订单数")
    private Integer shipOrderCount;

    @Schema(description = "当日发货订单实付金额（分）")
    private Long shipAmountFen;

    @Schema(description = "当日发货订单退款金额（分，独立统计）")
    private Long refundAmountFen;

    @Schema(description = "币种")
    private String currency;

}
