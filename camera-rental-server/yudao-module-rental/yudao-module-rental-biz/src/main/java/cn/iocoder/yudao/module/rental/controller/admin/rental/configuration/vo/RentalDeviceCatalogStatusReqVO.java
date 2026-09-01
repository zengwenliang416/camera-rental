package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalDeviceCatalogStatusReqVO {

    @NotNull
    private Long id;
    @NotNull
    private Boolean enabled;
    @NotNull
    private Integer lockVersion;

}
