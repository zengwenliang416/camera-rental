package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalDeviceModelUpdateReqVO {

    @NotNull
    private Long id;
    @NotNull
    private Long categoryId;
    @NotBlank
    private String modelCode;
    @NotBlank
    private String modelName;
    @NotBlank
    private String deviceNoPrefix;
    private Integer sortOrder;
    @NotNull
    private Integer lockVersion;

}
