package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCandidatesRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceScheduleDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalOrderScheduleDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalPendingAllocationOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalPendingAllocationPageReqVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalScheduleAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁排期分配读模型")
@RestController
@RequestMapping("/rental")
@Validated
public class RentalScheduleAllocationController {

    private final RentalScheduleAllocationService allocationService;

    public RentalScheduleAllocationController(RentalScheduleAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping("/order/pending-allocation-page")
    @Operation(summary = "分页查询待分配或部分分配订单")
    @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<PageResult<RentalPendingAllocationOrderRespVO>> getPendingAllocationPage(
            @Valid RentalPendingAllocationPageReqVO reqVO) {
        return success(allocationService.getPendingAllocationPage(reqVO));
    }

    @GetMapping("/order/{id}")
    @Operation(summary = "查询租赁订单排期详情")
    @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<RentalOrderScheduleDetailRespVO> getOrderScheduleDetail(
            @PathVariable("id") Long id) {
        return success(allocationService.getOrderScheduleDetail(id));
    }

    @GetMapping("/order-item/{itemId}/device-candidates")
    @Operation(summary = "查询订单明细的设备分配候选")
    @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<RentalDeviceCandidatesRespVO> getDeviceCandidates(
            @PathVariable("itemId") Long itemId) {
        return success(allocationService.getDeviceCandidates(itemId));
    }

    @GetMapping("/device/{id}/schedule-detail")
    @Operation(summary = "查询设备排期详情")
    @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<RentalDeviceScheduleDetailRespVO> getDeviceScheduleDetail(
            @PathVariable("id") Long id) {
        return success(allocationService.getDeviceScheduleDetail(id));
    }
}
