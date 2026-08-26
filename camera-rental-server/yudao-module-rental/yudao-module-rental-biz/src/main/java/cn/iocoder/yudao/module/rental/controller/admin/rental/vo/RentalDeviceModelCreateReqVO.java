package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 新增租赁设备型号")
@Data
public class RentalDeviceModelCreateReqVO {

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 64)
    private String modelCode;

    @NotBlank
    @Size(max = 128)
    private String modelName;

    @NotBlank
    @Size(max = 64)
    private String deviceNoPrefix;

    @Min(0)
    @Max(10000)
    private Integer sortOrder;

}
