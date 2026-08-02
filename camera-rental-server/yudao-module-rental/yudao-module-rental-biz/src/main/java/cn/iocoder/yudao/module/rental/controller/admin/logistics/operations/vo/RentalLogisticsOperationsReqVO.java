package cn.iocoder.yudao.module.rental.controller.admin.logistics.operations.vo;

import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.SecretAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

public final class RentalLogisticsOperationsReqVO {

    private RentalLogisticsOperationsReqVO() {
    }

    @Schema(description = "管理后台 - 物流 Provider 配置更新 Request VO")
    @Data
    public static class ProviderConfigUpdateReqVO {
        @NotBlank
        @Size(max = 32)
        private String providerCode;
        private Boolean enabled;
        private Boolean queryEnabled;
        private Boolean subscribeEnabled;
        private SecretAction callbackSecretAction;
        @Size(max = 512)
        private String callbackSecret;
        @Size(max = 512)
        private String callbackBaseUrl;
        @Min(1800)
        @Max(86400)
        private Integer minimumQueryIntervalSeconds;
        @Size(max = 16)
        private String resultVersion;
    }

    @Schema(description = "管理后台 - 物流 Provider 凭据保存 Request VO")
    @Data
    public static class ProviderCredentialSaveReqVO {
        private Long id;
        @NotBlank
        @Size(max = 32)
        private String providerCode;
        @NotBlank
        @Size(max = 64)
        private String credentialName;
        private Boolean enabled;
        @Min(0)
        @Max(10000)
        private Integer sortOrder;
        private SecretAction customerCodeAction;
        @Size(max = 512)
        private String customerCode;
        private SecretAction apiKeyAction;
        @Size(max = 512)
        private String apiKey;
    }

    @Schema(description = "管理后台 - 物流承运商映射保存 Request VO")
    @Data
    public static class CarrierMappingSaveReqVO {
        private Long id;
        @NotBlank
        @Size(max = 32)
        private String sourceType;
        @NotBlank
        @Size(max = 64)
        private String sourceCarrierCode;
        @NotBlank
        @Size(max = 64)
        private String canonicalCarrierCode;
        @NotBlank
        @Size(max = 128)
        private String displayName;
        @NotBlank
        @Size(max = 32)
        private String providerCode;
        @NotBlank
        @Size(max = 64)
        private String providerCarrierCode;
        @Size(max = 16)
        private String phoneRequirement;
        @Size(max = 16)
        private String status;
    }

    @Schema(description = "管理后台 - 物流 Reconcile Request VO")
    @Data
    public static class ReconcileReqVO {
        @Min(1)
        @Max(100)
        private Integer limit;
    }

    @Schema(description = "管理后台 - 历史物流回填 Request VO")
    @Data
    public static class BackfillReqVO {
        private Boolean dryRun;
        @Min(1)
        @Max(100)
        private Integer limit;
        private Boolean enqueueProviderTasks;
        @Schema(description = "发货日期开始，闭区间", example = "2026-07-30")
        private LocalDate consignDateStart;
        @Schema(description = "发货日期结束，闭区间", example = "2026-07-31")
        private LocalDate consignDateEnd;
    }

    @Schema(description = "管理后台 - 物流技术数据清理 Request VO")
    @Data
    public static class CleanupReqVO {
        private Boolean dryRun;
        @Min(30)
        @Max(3650)
        private Integer retentionDays;
        @Min(1)
        @Max(1000)
        private Integer limit;
    }
}
