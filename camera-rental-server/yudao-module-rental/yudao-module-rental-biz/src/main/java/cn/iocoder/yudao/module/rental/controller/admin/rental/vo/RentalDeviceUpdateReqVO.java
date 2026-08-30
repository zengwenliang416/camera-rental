package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 更新设备可变信息")
@Data
public class RentalDeviceUpdateReqVO {

    @NotNull
    private Long id;

    @Size(max = 128)
    private String serialNumber;

    @Size(max = 128)
    private String warehouseCode;

    @PositiveOrZero
    private Integer purchaseAmount;

    @NotNull
    private Boolean enabled;

}
