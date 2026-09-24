package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class RentalDeviceLabelReqVO {
    @NotEmpty @Size(max = 200) private List<@NotNull @Positive Long> deviceIds;
    @NotBlank @Size(max = 24) @Pattern(regexp = "[^\\p{Cntrl}]+") private String shopName;
    @NotBlank @Pattern(regexp = "[0-9+() -]{3,32}") private String phone;
    @NotBlank @Pattern(regexp = "zh-CN|en") private String locale;
}
