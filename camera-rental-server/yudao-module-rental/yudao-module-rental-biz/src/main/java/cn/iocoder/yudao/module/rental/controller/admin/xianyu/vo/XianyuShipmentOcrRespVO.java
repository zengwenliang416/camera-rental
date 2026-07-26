package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 发货图片 OCR/条码识别 Response VO")
@Data
public class XianyuShipmentOcrRespVO {

    private String waybillNo;
    private String expressCode;
    private String expressName;
    private BigDecimal confidence;
    private String extractionSource;

}
