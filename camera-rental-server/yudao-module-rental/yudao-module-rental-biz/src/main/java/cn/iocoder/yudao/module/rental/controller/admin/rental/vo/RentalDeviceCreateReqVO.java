package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "管理后台 - 创建设备")
@Data
public class RentalDeviceCreateReqVO {

    private String serialNumber;
    @NotBlank
    private String categoryCode;
    @NotBlank
    private String equipmentModelCode;
    @NotBlank
    @Pattern(regexp = "^(?:0?[1-9]|[1-9][0-9]{1,2})$")
    private String deviceNoSuffix;
    private String status;
    private String warehouseCode;
    private Integer purchaseAmount;
    private Boolean enabled;

}
