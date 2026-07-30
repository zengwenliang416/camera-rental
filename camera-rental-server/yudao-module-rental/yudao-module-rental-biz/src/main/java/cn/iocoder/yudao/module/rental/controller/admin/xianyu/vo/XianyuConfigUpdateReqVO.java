package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 闲管家租户配置更新 Request VO")
@Data
public class XianyuConfigUpdateReqVO {

    @NotNull
    private Boolean enabled;

    @NotBlank
    @Size(max = 512)
    private String baseUrl;

    @Size(max = 128)
    private String appKey;

    @Size(max = 512)
    private String appSecret;

    @Size(max = 512)
    private String webhookBaseUrl;

    @NotNull
    private Boolean writeEnabled;

    @NotNull
    private Boolean jobEnabled;

    @Min(1)
    @Max(180)
    private Integer lookbackDays;

    @Min(0)
    @Max(1440)
    private Integer overlapMinutes;

    @Min(1)
    @Max(100)
    private Integer maxPagesPerShop;

    @Min(1)
    @Max(100)
    private Integer pageSize;

    @Min(30)
    @Max(86400)
    private Integer pushRetryStaleSeconds;

    @Min(1)
    @Max(1000)
    private Integer pushRetryBatchSize;

}
