package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 租赁订单来源报表")
@Data
public class RentalReportSourceRespVO {

    private String sourceType;
    private Integer orderCount;
    private Long rentAmountFen;
    private Long refundAmountFen;

}
