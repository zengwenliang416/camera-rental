package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalSchedulePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchRespVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalScheduleAdminService;
import cn.iocoder.yudao.module.rental.service.admin.RentalScheduleWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁排期")
@RestController
@RequestMapping("/rental/schedule")
@Validated
public class RentalScheduleController {

    private final RentalScheduleAdminService scheduleAdminService;
    private final RentalScheduleWorkbenchService scheduleWorkbenchService;

    public RentalScheduleController(RentalScheduleAdminService scheduleAdminService,
                                    RentalScheduleWorkbenchService scheduleWorkbenchService) {
        this.scheduleAdminService = scheduleAdminService;
        this.scheduleWorkbenchService = scheduleWorkbenchService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询设备占用排期与计租周期")
    @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<PageResult<RentalScheduleRespVO>> getSchedulePage(
            @Valid RentalSchedulePageReqVO pageReqVO) {
        return success(scheduleAdminService.getSchedulePage(pageReqVO));
    }

    @GetMapping("/workbench")
    @Operation(summary = "查询设备排期工作台读模型")
    @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<RentalScheduleWorkbenchRespVO> getWorkbench(
            @Valid RentalScheduleWorkbenchReqVO reqVO) {
        return success(scheduleWorkbenchService.getWorkbench(reqVO));
    }

}
