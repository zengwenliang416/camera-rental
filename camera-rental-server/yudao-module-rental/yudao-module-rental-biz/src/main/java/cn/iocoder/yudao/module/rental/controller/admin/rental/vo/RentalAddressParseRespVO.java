package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 收货信息智能识别 Response VO")
@Data
public class RentalAddressParseRespVO {

    private String name;
    private String mobile;
    private String address;
    private String province;
    private String city;
    private String district;
    private String street;
    private String source;
    private Boolean fallback;
    private List<String> warnings;
}
