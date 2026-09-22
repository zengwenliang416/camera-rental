package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalDeviceQuantityUpdateReqVO {
    @NotNull @Min(1) @Max(999)
    private Integer quantity;
    @NotNull @Min(1)
    private Integer expectedQuantity;
}
