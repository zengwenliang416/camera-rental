package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备")
@Data
public class RentalDeviceRespVO {

    private Long id;
    private String deviceNo;
    private String serialNumber;
    private String equipmentModelCode;
    private String status;
    private String warehouseCode;
    private Integer purchaseAmount;
    private Boolean enabled;

}
