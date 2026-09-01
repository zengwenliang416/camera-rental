package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleImpactRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelReconciliationRunRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRulePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSaveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSaveRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleStatusReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductSkuRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalConfigurationCatalogRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalConfigurationShopRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalDeviceCatalogStatusReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalDeviceCategoryUpdateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalDeviceModelUpdateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalHistoricalBackfillCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalHistoricalBackfillRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalHistoricalBackfillResumeReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCategoryCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceModelCreateReqVO;
import cn.iocoder.yudao.module.rental.service.configuration.RentalChannelProductRuleService;
import cn.iocoder.yudao.module.rental.service.configuration.RentalConfigurationShopService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalHistoricalBackfillCommand;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelReconciliationRunService;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalHistoricalOrderBackfillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁配置")
@RestController
@RequestMapping("/rental/configuration")
@Validated
public class RentalConfigurationController {

    private final RentalDeviceCatalogService catalogService;
    private final RentalChannelProductRuleService productRuleService;
    private final RentalConfigurationShopService shopService;
    private final RentalChannelReconciliationRunService channelReconciliationRunService;
    private final RentalHistoricalOrderBackfillService historicalBackfillService;

    public RentalConfigurationController(RentalDeviceCatalogService catalogService,
                                         RentalChannelProductRuleService productRuleService,
                                         RentalConfigurationShopService shopService,
                                         RentalChannelReconciliationRunService channelReconciliationRunService,
                                         RentalHistoricalOrderBackfillService historicalBackfillService) {
        this.catalogService = catalogService;
        this.productRuleService = productRuleService;
        this.shopService = shopService;
        this.channelReconciliationRunService = channelReconciliationRunService;
        this.historicalBackfillService = historicalBackfillService;
    }

    @GetMapping("/catalog")
    @Operation(summary = "获取完整设备目录配置")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<RentalConfigurationCatalogRespVO> getCatalog() {
        return success(catalogService.getConfigurationCatalog());
    }

    @GetMapping("/shops")
    @Operation(summary = "获取租赁配置可用店铺")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<List<RentalConfigurationShopRespVO>> getConfigurationShops() {
        return success(shopService.getAvailableShops());
    }

    @PostMapping("/catalog/category/create")
    @Operation(summary = "新增设备大类")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<Long> createCategory(
            @Valid @RequestBody RentalDeviceCategoryCreateReqVO reqVO) {
        return success(catalogService.createCategory(reqVO));
    }

    @PutMapping("/catalog/category/update")
    @Operation(summary = "编辑设备大类")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<Integer> updateCategory(
            @Valid @RequestBody RentalDeviceCategoryUpdateReqVO reqVO) {
        return success(catalogService.updateCategory(reqVO));
    }

    @PutMapping("/catalog/category/status")
    @Operation(summary = "启停设备大类")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<Integer> updateCategoryStatus(
            @Valid @RequestBody RentalDeviceCatalogStatusReqVO reqVO) {
        return success(catalogService.updateCategoryStatus(reqVO));
    }

    @PostMapping("/catalog/model/create")
    @Operation(summary = "新增设备型号")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<Long> createModel(
            @Valid @RequestBody RentalDeviceModelCreateReqVO reqVO) {
        return success(catalogService.createModel(reqVO));
    }

    @PutMapping("/catalog/model/update")
    @Operation(summary = "编辑设备型号")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<Integer> updateModel(
            @Valid @RequestBody RentalDeviceModelUpdateReqVO reqVO) {
        return success(catalogService.updateModel(reqVO));
    }

    @PutMapping("/catalog/model/status")
    @Operation(summary = "启停设备型号")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<Integer> updateModelStatus(
            @Valid @RequestBody RentalDeviceCatalogStatusReqVO reqVO) {
        return success(catalogService.updateModelStatus(reqVO));
    }

    @GetMapping("/product-rule/page")
    @Operation(summary = "分页查询渠道商品规则")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<PageResult<RentalChannelProductRuleRespVO>> getProductRulePage(
            @Validated RentalChannelProductRulePageReqVO reqVO) {
        return success(productRuleService.getRulePage(reqVO));
    }

    @GetMapping("/product-rule/get")
    @Operation(summary = "获取渠道商品规则")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<RentalChannelProductRuleRespVO> getProductRule(
            @RequestParam("id") Long id) {
        return success(productRuleService.getRule(id));
    }

    @GetMapping("/product-rule/synced-skus")
    @Operation(summary = "获取商品已同步规格和当前映射")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<List<RentalChannelProductSkuRespVO>> getSyncedSkus(
            @RequestParam("shopId") Long shopId,
            @RequestParam("xianyuItemId") String xianyuItemId) {
        return success(productRuleService.getSynchronizedSkus(shopId, xianyuItemId));
    }

    @GetMapping("/product-rule/impact")
    @Operation(summary = "预览渠道商品规则影响")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<RentalChannelProductRuleImpactRespVO> previewProductRuleImpact(
            @RequestParam("shopId") Long shopId,
            @RequestParam("xianyuItemId") String xianyuItemId) {
        return success(productRuleService.previewImpact(shopId, xianyuItemId));
    }

    @PostMapping("/product-rule/create")
    @Operation(summary = "新增渠道商品规则")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<RentalChannelProductRuleSaveRespVO> createProductRule(
            @Valid @RequestBody RentalChannelProductRuleSaveReqVO reqVO) {
        return success(productRuleService.createRule(reqVO));
    }

    @PutMapping("/product-rule/update")
    @Operation(summary = "编辑渠道商品规则")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<RentalChannelProductRuleSaveRespVO> updateProductRule(
            @Valid @RequestBody RentalChannelProductRuleSaveReqVO reqVO) {
        return success(productRuleService.updateRule(reqVO));
    }

    @PutMapping("/product-rule/status")
    @Operation(summary = "启停渠道商品规则")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<RentalChannelProductRuleSaveRespVO> updateProductRuleStatus(
            @Valid @RequestBody RentalChannelProductRuleStatusReqVO reqVO) {
        return success(productRuleService.updateRuleStatus(reqVO));
    }

    @GetMapping("/product-rule/reconciliation")
    @Operation(summary = "查询渠道商品规则重评结果")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<RentalChannelReconciliationRunRespVO> getProductRuleReconciliation(
            @RequestParam("runId") @Min(1) Long runId) {
        return success(RentalChannelReconciliationRunRespVO.from(
                channelReconciliationRunService.get(runId)));
    }

    @PostMapping("/historical-reconciliation/run")
    @Operation(summary = "创建并执行有界历史订单补建任务")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<RentalHistoricalBackfillRespVO> runHistoricalReconciliation(
            @Valid @RequestBody RentalHistoricalBackfillCreateReqVO reqVO) {
        return success(RentalHistoricalBackfillRespVO.from(
                historicalBackfillService.createAndRun(new RentalHistoricalBackfillCommand(
                        reqVO.getStartAfterId(),
                        reqVO.getEndIdInclusive(),
                        reqVO.getBatchSize(),
                        reqVO.getMaxBatches(),
                        reqVO.getDryRun(),
                        reqVO.getConfirmation()))));
    }

    @GetMapping("/historical-reconciliation/get")
    @Operation(summary = "查询历史订单补建任务")
    @PreAuthorize("@ss.hasPermission('rental:configuration:query')")
    public CommonResult<RentalHistoricalBackfillRespVO> getHistoricalReconciliation(
            @RequestParam("id") @Min(1) Long id) {
        return success(RentalHistoricalBackfillRespVO.from(
                historicalBackfillService.get(id)));
    }

    @PutMapping("/historical-reconciliation/pause")
    @Operation(summary = "暂停历史订单补建任务")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<RentalHistoricalBackfillRespVO> pauseHistoricalReconciliation(
            @RequestParam("id") @Min(1) Long id) {
        return success(RentalHistoricalBackfillRespVO.from(
                historicalBackfillService.pause(id)));
    }

    @PutMapping("/historical-reconciliation/resume")
    @Operation(summary = "恢复历史订单补建任务")
    @PreAuthorize("@ss.hasPermission('rental:configuration:update')")
    public CommonResult<RentalHistoricalBackfillRespVO> resumeHistoricalReconciliation(
            @Valid @RequestBody RentalHistoricalBackfillResumeReqVO reqVO) {
        return success(RentalHistoricalBackfillRespVO.from(
                historicalBackfillService.resume(
                        reqVO.getRunId(),
                        reqVO.getMaxBatches(),
                        reqVO.getConfirmation())));
    }
}
