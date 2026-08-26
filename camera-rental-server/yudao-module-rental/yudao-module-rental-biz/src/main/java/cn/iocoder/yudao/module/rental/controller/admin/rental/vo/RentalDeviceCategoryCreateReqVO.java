package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 新增租赁设备大类")
@Data
public class RentalDeviceCategoryCreateReqVO {

    @NotBlank
    @Size(max = 32)
    private String categoryCode;

    @NotBlank
    @Size(max = 64)
    private String categoryName;

    @Min(0)
    @Max(10000)
    private Integer sortOrder;

}
