package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 租金收入汇总（分）")
@Data
public class RentalRevenueReportRespVO {

    private Integer orderCount;
    private Long rentAmountFen;
    private Long refundAmountFen;
    private String currency;

}
