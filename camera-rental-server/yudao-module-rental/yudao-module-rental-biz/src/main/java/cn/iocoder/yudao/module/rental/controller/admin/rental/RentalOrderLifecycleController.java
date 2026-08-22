package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalOrderCancelReqVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalOrderLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁订单生命周期")
@RestController
@RequestMapping("/rental/order")
@Validated
public class RentalOrderLifecycleController {

    private final RentalOrderLifecycleService orderLifecycleService;

    public RentalOrderLifecycleController(RentalOrderLifecycleService orderLifecycleService) {
        this.orderLifecycleService = orderLifecycleService;
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消租赁订单并释放未出库分配")
    @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<Boolean> cancelOrder(@Valid @RequestBody RentalOrderCancelReqVO reqVO) {
        orderLifecycleService.cancelOrder(reqVO.getOrderId(), reqVO.getReason());
        return success(true);
    }

}
