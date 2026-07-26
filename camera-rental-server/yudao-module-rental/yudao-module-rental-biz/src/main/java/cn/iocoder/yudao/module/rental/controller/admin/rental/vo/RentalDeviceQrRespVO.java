package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备永久二维码")
@Data
public class RentalDeviceQrRespVO {

    private Long deviceId;
    private String deviceNo;
    private String equipmentModelCode;
    /** Permanent QR payload string — print this; content is stable for the device. */
    private String payload;
    private String payloadVersion;
    private boolean signed;

}
