package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncRespVO;
import cn.iocoder.yudao.module.rental.service.RentalConversionResult;
import cn.iocoder.yudao.module.rental.service.admin.XianyuOrderAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "管理后台 - 闲鱼订单")
@RestController
@RequestMapping("/rental/xianyu/order")
@Validated
public class XianyuOrderController {

    private final XianyuOrderAdminService orderAdminService;

    public XianyuOrderController(XianyuOrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询渠道订单（无敏感地址/手机）")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<PageResult<XianyuOrderRespVO>> getOrderPage(
            @Valid XianyuOrderPageReqVO pageReqVO) {
        return success(orderAdminService.getOrderPage(pageReqVO));
    }

    @PostMapping("/sync-page")
    @Operation(summary = "有界只读订单同步一页")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:sync')")
    public CommonResult<XianyuOrderSyncRespVO> syncPage(@Valid @RequestBody XianyuOrderSyncReqVO reqVO) {
        return success(orderAdminService.syncPage(reqVO));
    }

    @PostMapping("/convert")
    @Operation(summary = "转换渠道订单为内部租赁订单")
    @PreAuthorize("@ss.hasPermission('rental:order:convert')")
    @Parameter(name = "channelOrderId", required = true)
    public CommonResult<RentalConversionResult> convert(@RequestParam("channelOrderId") Long channelOrderId) {
        return success(orderAdminService.convert(channelOrderId));
    }

}
