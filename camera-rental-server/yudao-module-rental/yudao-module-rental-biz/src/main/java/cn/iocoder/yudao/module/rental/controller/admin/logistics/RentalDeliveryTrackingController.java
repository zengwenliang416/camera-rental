package cn.iocoder.yudao.module.rental.controller.admin.logistics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryRefreshRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingBatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingOrderSummaryRespVO;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryRefreshResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryTrackingQueryService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryTrackingRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁物流跟踪")
@RestController
@RequestMapping("/rental/delivery")
@Validated
public class RentalDeliveryTrackingController {

    private final RentalDeliveryTrackingQueryService queryService;
    private final RentalDeliveryTrackingRefreshService refreshService;

    public RentalDeliveryTrackingController(RentalDeliveryTrackingQueryService queryService,
                                            RentalDeliveryTrackingRefreshService refreshService) {
        this.queryService = queryService;
        this.refreshService = refreshService;
    }

    @PostMapping("/tracking-summary/batch")
    @Operation(summary = "按订单批量查询本地物流摘要")
    @PreAuthorize("@ss.hasPermission('rental:delivery:tracking')")
    public CommonResult<Map<Long, RentalDeliveryTrackingOrderSummaryRespVO>> getTrackingSummaries(
            @Valid @RequestBody RentalDeliveryTrackingBatchReqVO reqVO) {
        return success(queryService.getSummaries(reqVO.getOrderIds()));
    }

    @GetMapping("/{deliveryId}/tracking")
    @Operation(summary = "按需查询包裹当前完整物流快照")
    @PreAuthorize("@ss.hasPermission('rental:delivery:tracking')")
    public CommonResult<RentalDeliveryTrackingDetailRespVO> getTracking(
            @PathVariable("deliveryId") Long deliveryId) {
        return success(queryService.getDetail(deliveryId));
    }

    @PostMapping("/{deliveryId}/refresh")
    @Operation(summary = "提交本地物流异步刷新")
    @PreAuthorize("@ss.hasPermission('rental:delivery:tracking')")
    public CommonResult<RentalDeliveryRefreshRespVO> refresh(
            @PathVariable("deliveryId") Long deliveryId) {
        RentalDeliveryRefreshResult refreshResult = refreshService.refresh(deliveryId);
        RentalDeliveryRefreshRespVO response = new RentalDeliveryRefreshRespVO();
        response.setAccepted(refreshResult.accepted());
        response.setReason(refreshResult.reason());
        response.setNextAllowedAt(refreshResult.nextAllowedAt());
        return success(response);
    }
}
