package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceAssignReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceRespVO;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁设备")
@RestController
@RequestMapping("/rental/device")
@Validated
public class RentalDeviceController {

    private final RentalDeviceAdminService deviceAdminService;

    public RentalDeviceController(RentalDeviceAdminService deviceAdminService) {
        this.deviceAdminService = deviceAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询设备")
    @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<PageResult<RentalDeviceRespVO>> getDevicePage(
            @RequestParam(value = "equipmentModelCode", required = false) String equipmentModelCode,
            @Validated PageParam pageParam) {
        return success(deviceAdminService.getDevicePage(equipmentModelCode, pageParam));
    }

    @PostMapping("/create")
    @Operation(summary = "创建设备实例")
    @PreAuthorize("@ss.hasPermission('rental:device:create')")
    public CommonResult<Long> createDevice(@Valid @RequestBody RentalDeviceCreateReqVO reqVO) {
        return success(deviceAdminService.createDevice(reqVO));
    }

    @PostMapping("/assign")
    @Operation(summary = "分配设备并创建占用排期")
    @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<RentalDeviceAssignmentResult> assign(@Valid @RequestBody RentalDeviceAssignReqVO reqVO) {
        return success(deviceAdminService.assign(new RentalDeviceAssignmentCommand(
                reqVO.getRentalOrderItemId(),
                reqVO.getDeviceId(),
                reqVO.getOccupyStartDate(),
                reqVO.getOccupyEndDateExclusive(),
                reqVO.getIdempotencyKey())));
    }

}
