package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 线下客户反查结果")
@Data
public class RentalCustomerSuggestRespVO {

    @Schema(description = "客户 ID")
    private Long id;

    @Schema(description = "客户姓名")
    private String name;

    @Schema(description = "客户手机号")
    private String mobile;

    @Schema(description = "微信号")
    private String wechatId;

}
