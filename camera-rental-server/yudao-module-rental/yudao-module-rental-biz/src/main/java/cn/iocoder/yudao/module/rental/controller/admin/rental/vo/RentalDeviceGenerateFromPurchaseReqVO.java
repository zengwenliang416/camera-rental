package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 从 ERP 采购入库生成租赁设备")
@Data
public class RentalDeviceGenerateFromPurchaseReqVO {

    @NotNull
    @Schema(description = "ERP 采购入库单 ID")
    private Long purchaseInId;

    @Schema(description = "可选：仅处理该入库明细；空则处理整单")
    private Long purchaseInItemId;

    @Schema(description = "设备编号前缀，默认用产品条码/名称")
    private String deviceNoPrefix;

    @Schema(description = "型号编码覆盖；默认用产品条码，无则用产品名")
    private String equipmentModelCode;

    @Schema(description = "仓库编码覆盖；默认用 ERP 仓库名")
    private String warehouseCode;

}
