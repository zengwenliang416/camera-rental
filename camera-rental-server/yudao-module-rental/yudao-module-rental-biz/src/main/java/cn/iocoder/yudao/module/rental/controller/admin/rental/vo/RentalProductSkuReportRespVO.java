package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 租赁商品与 SKU 经营报表")
@Data
public class RentalProductSkuReportRespVO {

    private Long shopId;
    private String externalProductId;
    private String externalSkuId;
    private String goodsTitle;
    private Integer orderCount;
    private Long goodsQuantity;
    private Long rentAmountFen;
    private Long refundAmountFen;

}
