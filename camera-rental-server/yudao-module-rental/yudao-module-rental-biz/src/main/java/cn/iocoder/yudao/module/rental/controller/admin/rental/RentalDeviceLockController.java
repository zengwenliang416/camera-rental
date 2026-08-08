package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLockCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLockReleaseReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLockRespVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 租赁设备锁定")
@RestController
@RequestMapping("/rental/device-lock")
@Validated
public class RentalDeviceLockController {

    private final RentalDeviceLockService lockService;

    public RentalDeviceLockController(RentalDeviceLockService lockService) {
        this.lockService = lockService;
    }

    @PostMapping
    @Operation(summary = "创建订单预留或主管保留")
    @PreAuthorize("@ss.hasPermission('rental:device-lock:update')")
    public CommonResult<RentalDeviceLockRespVO> create(@Valid @RequestBody RentalDeviceLockCreateReqVO reqVO) {
        return success(lockService.createManualLock(reqVO));
    }

    @PutMapping("/{id}/release")
    @Operation(summary = "解除订单预留或主管保留")
    @PreAuthorize("@ss.hasPermission('rental:device-lock:update')")
    public CommonResult<RentalDeviceLockRespVO> release(@PathVariable("id") Long id,
                                                        @Valid @RequestBody RentalDeviceLockReleaseReqVO reqVO) {
        return success(lockService.releaseManualLock(id, getLoginUserId(), reqVO.getReason()));
    }
}
