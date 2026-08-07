package cn.iocoder.yudao.module.rental.controller.admin.logistics.operations;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.operations.vo.RentalLogisticsOperationsReqVO.CarrierMappingSaveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.operations.vo.RentalLogisticsOperationsReqVO.BackfillReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.operations.vo.RentalLogisticsOperationsReqVO.CleanupReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.operations.vo.RentalLogisticsOperationsReqVO.ProviderConfigUpdateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.operations.vo.RentalLogisticsOperationsReqVO.ProviderCredentialSaveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.operations.vo.RentalLogisticsOperationsReqVO.ReconcileReqVO;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsBackfillService;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsCleanupService;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsConfigurationOperationsService;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsMetricsService;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.BackfillCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.BackfillResult;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.CarrierMappingCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.CarrierMappingView;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.CleanupCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.CleanupResult;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.FailedTaskView;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.MetricsView;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderConfigCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderConfigView;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderCredentialCommand;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderCredentialView;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ProviderVerifyResult;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.ReconcileResult;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.RetryResult;
import cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsTaskOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁物流运营")
@RestController
@RequestMapping("/rental/logistics/operations")
@Validated
public class RentalLogisticsOperationsController {

    private final RentalLogisticsConfigurationOperationsService configurationService;
    private final RentalLogisticsTaskOperationsService taskService;
    private final RentalLogisticsMetricsService metricsService;
    private final RentalLogisticsBackfillService backfillService;
    private final RentalLogisticsCleanupService cleanupService;

    public RentalLogisticsOperationsController(
            RentalLogisticsConfigurationOperationsService configurationService,
            RentalLogisticsTaskOperationsService taskService,
            RentalLogisticsMetricsService metricsService,
            RentalLogisticsBackfillService backfillService,
            RentalLogisticsCleanupService cleanupService) {
        this.configurationService = configurationService;
        this.taskService = taskService;
        this.metricsService = metricsService;
        this.backfillService = backfillService;
        this.cleanupService = cleanupService;
    }

    @GetMapping("/provider-config/{providerCode}")
    @Operation(summary = "查询脱敏 Provider 配置")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ProviderConfigView> getProviderConfig(
            @PathVariable("providerCode") String providerCode) {
        return success(configurationService.getProviderConfig(providerCode));
    }

    @PutMapping("/provider-config")
    @Operation(summary = "保存 Provider 公共配置，回调密钥仅支持保留、替换或清除")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ProviderConfigView> saveProviderConfig(
            @Valid @RequestBody ProviderConfigUpdateReqVO reqVO) {
        ProviderConfigCommand command = new ProviderConfigCommand(reqVO.getProviderCode(), reqVO.getEnabled(),
                reqVO.getQueryEnabled(), reqVO.getSubscribeEnabled(),
                reqVO.getCallbackSecretAction(), reqVO.getCallbackSecret(), reqVO.getCallbackBaseUrl(),
                reqVO.getMinimumQueryIntervalSeconds(), reqVO.getResultVersion());
        return success(configurationService.saveProviderConfig(command));
    }

    @PutMapping("/provider-credential")
    @Operation(summary = "新增或更新当前租户 Provider 凭据")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ProviderCredentialView> saveProviderCredential(
            @Valid @RequestBody ProviderCredentialSaveReqVO reqVO) {
        ProviderCredentialCommand command = new ProviderCredentialCommand(reqVO.getId(),
                reqVO.getProviderCode(), reqVO.getCredentialName(), reqVO.getEnabled(),
                reqVO.getSortOrder(), reqVO.getCustomerCodeAction(), reqVO.getCustomerCode(),
                reqVO.getApiKeyAction(), reqVO.getApiKey());
        return success(configurationService.saveProviderCredential(command));
    }

    @DeleteMapping("/provider-credential/{id}")
    @Operation(summary = "删除当前租户 Provider 凭据")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<Boolean> deleteProviderCredential(@PathVariable("id") Long id) {
        configurationService.deleteProviderCredential(id);
        return success(true);
    }

    @PostMapping("/provider-credential/{id}/verify")
    @Operation(summary = "执行 Provider 凭据本地完整性验证，不访问供应商网络")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ProviderVerifyResult> verifyProviderCredential(@PathVariable("id") Long id) {
        return success(configurationService.verifyProviderCredential(id));
    }

    @PostMapping("/provider-config/{providerCode}/verify")
    @Operation(summary = "执行 Provider 配置本地完整性验证，不访问供应商网络")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ProviderVerifyResult> verifyProviderConfig(
            @PathVariable("providerCode") String providerCode) {
        return success(configurationService.verifyProviderConfig(providerCode));
    }

    @GetMapping("/carrier-mapping")
    @Operation(summary = "查询当前租户承运商映射")
    @PreAuthorize("@ss.hasPermission('rental:logistics:mapping:query')")
    public CommonResult<List<CarrierMappingView>> listCarrierMappings() {
        return success(configurationService.listCarrierMappings());
    }

    @PutMapping("/carrier-mapping")
    @Operation(summary = "新增或更新当前租户承运商映射")
    @PreAuthorize("@ss.hasPermission('rental:logistics:mapping:update')")
    public CommonResult<CarrierMappingView> saveCarrierMapping(
            @Valid @RequestBody CarrierMappingSaveReqVO reqVO) {
        return success(configurationService.saveCarrierMapping(new CarrierMappingCommand(reqVO.getId(),
                reqVO.getSourceType(), reqVO.getSourceCarrierCode(), reqVO.getCanonicalCarrierCode(),
                reqVO.getDisplayName(), reqVO.getProviderCode(), reqVO.getProviderCarrierCode(),
                reqVO.getPhoneRequirement(), reqVO.getStatus())));
    }

    @DeleteMapping("/carrier-mapping/{id}")
    @Operation(summary = "删除当前租户承运商映射")
    @PreAuthorize("@ss.hasPermission('rental:logistics:mapping:delete')")
    public CommonResult<Boolean> deleteCarrierMapping(@PathVariable("id") Long id) {
        configurationService.deleteCarrierMapping(id);
        return success(true);
    }

    @GetMapping("/failed-task")
    @Operation(summary = "查询脱敏失败 Inbox/Outbox 任务")
    @PreAuthorize("@ss.hasPermission('rental:logistics:task:query')")
    public CommonResult<List<FailedTaskView>> listFailedTasks(
            @RequestParam(value = "taskType", required = false) String taskType,
            @RequestParam(value = "limit", required = false) @Min(1) @Max(100) Integer limit) {
        return success(taskService.listFailedTasks(taskType, limit));
    }

    @PostMapping("/failed-task/{taskType}/{id}/retry")
    @Operation(summary = "安全重试当前租户失败物流任务")
    @PreAuthorize("@ss.hasPermission('rental:logistics:task:retry')")
    public CommonResult<RetryResult> retryFailedTask(
            @PathVariable("taskType") String taskType,
            @PathVariable("id") Long id) {
        return success(taskService.retry(taskType, id));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "有界提交 Reconcile Outbox，不同步调用 Provider")
    @PreAuthorize("@ss.hasPermission('rental:logistics:reconcile')")
    public CommonResult<ReconcileResult> reconcile(@Valid @RequestBody(required = false) ReconcileReqVO reqVO) {
        return success(taskService.reconcile(reqVO == null ? null : reqVO.getLimit()));
    }

    @GetMapping("/metrics")
    @Operation(summary = "查询不含隐私字段的物流运营指标")
    @PreAuthorize("@ss.hasPermission('rental:logistics:metrics:query')")
    public CommonResult<MetricsView> getMetrics() {
        return success(metricsService.getMetrics());
    }

    @PostMapping("/backfill")
    @Operation(summary = "执行 dry-run 或有界历史 shipment 到 Delivery 回填")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<BackfillResult> backfill(@Valid @RequestBody(required = false) BackfillReqVO reqVO) {
        BackfillCommand command = reqVO == null
                ? new BackfillCommand(true, 20, false)
                : new BackfillCommand(reqVO.getDryRun() == null || reqVO.getDryRun(),
                reqVO.getLimit() == null ? 20 : reqVO.getLimit(),
                Boolean.TRUE.equals(reqVO.getEnqueueProviderTasks()),
                reqVO.getConsignDateStart(), reqVO.getConsignDateEnd());
        return success(backfillService.backfill(command));
    }

    @PostMapping("/cleanup")
    @Operation(summary = "dry-run 或有界清理过期且已处理的物流技术数据")
    @PreAuthorize("@ss.hasPermission('rental:logistics:cleanup')")
    public CommonResult<CleanupResult> cleanup(@Valid @RequestBody(required = false) CleanupReqVO reqVO) {
        CleanupCommand command = reqVO == null
                ? new CleanupCommand(true, 90, 500)
                : new CleanupCommand(reqVO.getDryRun() == null || reqVO.getDryRun(),
                reqVO.getRetentionDays() == null ? 90 : reqVO.getRetentionDays(),
                reqVO.getLimit() == null ? 500 : reqVO.getLimit());
        return success(cleanupService.cleanup(command));
    }
}
