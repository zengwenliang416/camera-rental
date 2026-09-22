package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceQuantityUpdateReqVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalOrderQuantityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/rental/order-item")
@Validated
@RequiredArgsConstructor
public class RentalOrderQuantityController {
    private final RentalOrderQuantityService quantityService;

    @PutMapping("/{id}/quantity")
    @PreAuthorize("@ss.hasAnyPermissions('rental:device:assign', 'rental:xianyu:ship')")
    public CommonResult<Integer> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody RentalDeviceQuantityUpdateReqVO request) {
        return success(quantityService.update(id, request));
    }
}
