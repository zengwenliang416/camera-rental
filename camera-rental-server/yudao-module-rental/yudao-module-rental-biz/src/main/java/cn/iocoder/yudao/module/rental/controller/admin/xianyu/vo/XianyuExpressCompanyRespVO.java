package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 闲管家快递公司")
@Data
public class XianyuExpressCompanyRespVO {

    private String code;
    private String expressName;
    private String expressAlias;
    private Boolean hot;

}
