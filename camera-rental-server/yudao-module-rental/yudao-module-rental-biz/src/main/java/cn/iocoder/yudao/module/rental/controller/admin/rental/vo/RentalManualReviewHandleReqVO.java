package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 处理人工复核 Request VO")
@Data
public class RentalManualReviewHandleReqVO {

    @Schema(description = "复核记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "复核记录编号不能为空")
    private Long id;

    @Schema(description = "处理备注", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处理备注不能为空")
    @Size(max = 512, message = "处理备注长度不能超过 512 个字符")
    private String resolutionNote;

}
