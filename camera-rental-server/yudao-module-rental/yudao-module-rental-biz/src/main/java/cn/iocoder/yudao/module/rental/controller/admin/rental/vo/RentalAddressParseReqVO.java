package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 收货信息智能识别 Request VO")
@Data
public class RentalAddressParseReqVO {

    @Schema(description = "包含姓名、手机号和地址的原始文本", required = true)
    @NotBlank
    @Size(max = 1000)
    private String text;
}
