package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDevicePerformanceReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalProductSkuReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportOverviewRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportQueryReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalRevenueReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalShipDateSummaryRespVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalReportAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁报表")
@RestController
@RequestMapping("/rental/report")
@Validated
public class RentalReportController {

    private final RentalReportAdminService reportAdminService;

    public RentalReportController(RentalReportAdminService reportAdminService) {
        this.reportAdminService = reportAdminService;
    }

    @GetMapping("/revenue-summary")
    @Operation(summary = "租金收入汇总（pay_amount 为租金，单位分）")
    @PreAuthorize("@ss.hasPermission('rental:report:query')")
    public CommonResult<RentalRevenueReportRespVO> getRevenueSummary(
            @RequestParam(value = "shopId", required = false) Long shopId) {
        return success(reportAdminService.getRevenueSummary(shopId));
    }

    @GetMapping("/overview")
    @Operation(summary = "获取租赁经营报表总览")
    @PreAuthorize("@ss.hasPermission('rental:report:query')")
    public CommonResult<RentalReportOverviewRespVO> getOverview(
            @Valid RentalReportQueryReqVO reqVO) {
        return success(reportAdminService.getOverview(reqVO));
    }

    @GetMapping("/ship-date-summary")
    @Operation(summary = "按发货日统计金额（按 ship_date 过滤，单位分）")
    @PreAuthorize("@ss.hasPermission('rental:report:query')")
    public CommonResult<RentalShipDateSummaryRespVO> getShipDateSummary(
            @RequestParam("date") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return success(reportAdminService.getShipDateSummary(date));
    }

    @GetMapping("/product-sku-page")
    @Operation(summary = "分页获取商品与 SKU 收入报表")
    @PreAuthorize("@ss.hasPermission('rental:report:query')")
    public CommonResult<PageResult<RentalProductSkuReportRespVO>> getProductSkuPage(
            @Valid RentalReportQueryReqVO reqVO) {
        return success(reportAdminService.getProductSkuPage(reqVO));
    }

    @GetMapping("/device-performance-page")
    @Operation(summary = "分页获取设备利用率、闲置时间与已分配收入")
    @PreAuthorize("@ss.hasPermission('rental:report:query')")
    public CommonResult<PageResult<RentalDevicePerformanceReportRespVO>> getDevicePerformancePage(
            @Valid RentalReportQueryReqVO reqVO) {
        return success(reportAdminService.getDevicePerformancePage(reqVO));
    }

}
