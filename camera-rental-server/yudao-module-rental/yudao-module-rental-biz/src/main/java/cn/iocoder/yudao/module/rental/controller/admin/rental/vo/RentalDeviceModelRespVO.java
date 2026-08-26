package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "管理后台 - 租赁设备型号")
@Data
@AllArgsConstructor
public class RentalDeviceModelRespVO {

    private Long id;
    private String modelCode;
    private String modelName;
    private String deviceNoPrefix;

}
