package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 闲鱼售后")
@Data
public class XianyuAfterSaleRespVO {

    private Long id;
    private Long shopId;
    private String externalAfterSaleId;
    private String externalOrderId;
    private String afterSaleStatus;
    private Long refundAmount;
    private String amountUnitStatus;
    private LocalDateTime timeoutAt;
    private LocalDateTime sourceUpdatedAt;

}
