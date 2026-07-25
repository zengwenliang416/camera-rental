package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 创建设备")
@Data
public class RentalDeviceCreateReqVO {

    @NotBlank
    private String deviceNo;
    private String serialNumber;
    @NotBlank
    private String equipmentModelCode;
    private String status;
    private String warehouseCode;
    private Integer purchaseAmount;
    private Boolean enabled;

}
