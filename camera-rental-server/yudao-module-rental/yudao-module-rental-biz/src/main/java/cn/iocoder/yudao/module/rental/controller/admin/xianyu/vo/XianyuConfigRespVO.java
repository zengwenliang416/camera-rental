package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 闲管家集成配置状态（脱敏）")
@Data
public class XianyuConfigRespVO {

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "集成状态 DISABLED/MISSING_CREDENTIALS/READY")
    private String status;

    @Schema(description = "基础地址")
    private String baseUrl;

    @Schema(description = "脱敏 AppKey")
    private String appKeyMasked;

    @Schema(description = "是否已配置 AppSecret（永不返回明文）")
    private Boolean appSecretConfigured;

    @Schema(description = "是否已配置 webhook 基址")
    private Boolean webhookBaseUrlConfigured;

    @Schema(description = "是否开启闲管家写操作")
    private Boolean writeEnabled;

    @Schema(description = "Webhook 基址")
    private String webhookBaseUrl;

    @Schema(description = "是否启用同步任务")
    private Boolean jobEnabled;

    private Integer lookbackDays;
    private Integer overlapMinutes;
    private Integer maxPagesPerShop;
    private Integer pageSize;
    private Integer pushRetryStaleSeconds;
    private Integer pushRetryBatchSize;

}
