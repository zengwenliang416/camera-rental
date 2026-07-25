package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 闲鱼售后分页请求")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class XianyuAfterSalePageReqVO extends PageParam {

    private Long shopId;
    private String afterSaleStatus;

}
