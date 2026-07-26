package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 解析设备永久二维码")
@Data
public class RentalDeviceResolveQrReqVO {

    @NotBlank
    private String payload;

}
