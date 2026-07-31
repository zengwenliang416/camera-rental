package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物流关联设备")
@Data
public class RentalDeliveryTrackingDeviceRespVO {

    private Long deviceId;
    private String deviceNo;
    private String equipmentModelCode;
}
