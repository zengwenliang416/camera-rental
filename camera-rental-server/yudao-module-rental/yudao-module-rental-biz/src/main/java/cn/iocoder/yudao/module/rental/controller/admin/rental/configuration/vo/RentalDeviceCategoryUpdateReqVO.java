package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalDeviceCategoryUpdateReqVO {

    @NotNull
    private Long id;
    @NotBlank
    private String categoryCode;
    @NotBlank
    private String categoryName;
    private Integer sortOrder;
    @NotNull
    private Integer lockVersion;

}
