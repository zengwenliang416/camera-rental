package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class RentalDeviceImportReqVO {
    @NotBlank @Pattern(regexp = "REPRINT|CREATE")
    private String mode;
    @NotEmpty @Size(max = 200) @Valid
    private List<@NotNull Row> rows;

    @Data
    public static class Row {
        @NotBlank @Size(max = 128) private String fileName;
        @NotNull @Min(1) private Integer lineNumber;
        @NotBlank @Size(max = 32) private String categoryCode;
        @NotBlank @Size(max = 64) private String equipmentModelCode;
        @Size(max = 64) private String deviceNo;
        @Size(max = 128) private String serialNumber;
    }
}
