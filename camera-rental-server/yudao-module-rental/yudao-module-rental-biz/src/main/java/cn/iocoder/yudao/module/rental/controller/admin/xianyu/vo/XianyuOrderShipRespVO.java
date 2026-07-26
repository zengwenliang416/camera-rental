package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 闲鱼订单发货 Response VO")
@Data
public class XianyuOrderShipRespVO {

    private Long shipmentId;
    private Long channelOrderId;
    private Long assignmentId;
    private Long deviceId;
    private String deviceNo;
    private String maskedWaybillNo;
    private String expressCode;
    private String expressName;
    private Integer remoteCode;
    private String remoteMsg;
    private String assignmentStatus;
    private String source;

}
