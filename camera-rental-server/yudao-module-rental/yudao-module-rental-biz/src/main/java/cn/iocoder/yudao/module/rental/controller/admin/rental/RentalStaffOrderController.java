package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffOrderRespVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalStaffOrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/rental/order")
@Validated
public class RentalStaffOrderController {
    private final RentalStaffOrderService service;
    public RentalStaffOrderController(RentalStaffOrderService service) { this.service = service; }

    @GetMapping("/staff-page")
    @Operation(summary = "员工订单查询：收件信息、发货状态及模糊搜索")
    @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<PageResult<RentalStaffOrderRespVO>> getPage(@Valid RentalStaffOrderPageReqVO req) {
        return success(service.getPage(req));
    }
}
