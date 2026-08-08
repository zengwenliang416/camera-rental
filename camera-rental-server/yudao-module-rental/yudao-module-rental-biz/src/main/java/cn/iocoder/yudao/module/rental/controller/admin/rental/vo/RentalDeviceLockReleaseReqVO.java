package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RentalDeviceLockReleaseReqVO {

    @NotBlank
    @Size(max = 512)
    private String reason;
}
