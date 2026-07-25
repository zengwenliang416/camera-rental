package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 闲鱼店铺")
@Data
public class XianyuShopRespVO {

    private Long id;
    private Long applicationId;
    private String externalShopId;
    private String authorizeId;
    private String shopName;
    private String authorizationStatus;
    private LocalDateTime authorizationExpiresAt;
    private String guaranteeStatus;

}
