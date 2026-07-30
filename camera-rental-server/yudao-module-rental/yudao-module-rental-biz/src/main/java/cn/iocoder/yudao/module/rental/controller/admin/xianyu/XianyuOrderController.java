package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderShipRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPendingShipOrderRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuShipmentOcrRespVO;
import cn.iocoder.yudao.module.rental.service.RentalConversionResult;
import cn.iocoder.yudao.module.rental.service.admin.ShipmentOcrService;
import cn.iocoder.yudao.module.rental.service.admin.XianyuOrderAdminService;
import cn.iocoder.yudao.module.rental.service.admin.XianyuOrderRemarkReparseService;
import cn.iocoder.yudao.module.rental.service.admin.XianyuOrderShipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲鱼订单")
@RestController
@RequestMapping("/rental/xianyu/order")
@Validated
public class XianyuOrderController {

    private final XianyuOrderAdminService orderAdminService;
    private final XianyuOrderRemarkReparseService orderRemarkReparseService;
    private final XianyuOrderShipService orderShipService;
    private final ShipmentOcrService shipmentOcrService;

    public XianyuOrderController(XianyuOrderAdminService orderAdminService,
                                 XianyuOrderRemarkReparseService orderRemarkReparseService,
                                 XianyuOrderShipService orderShipService,
                                 ShipmentOcrService shipmentOcrService) {
        this.orderAdminService = orderAdminService;
        this.orderRemarkReparseService = orderRemarkReparseService;
        this.orderShipService = orderShipService;
        this.shipmentOcrService = shipmentOcrService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询渠道订单（含完整收货快照）")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<PageResult<XianyuOrderRespVO>> getOrderPage(
            @Valid XianyuOrderPageReqVO pageReqVO) {
        return success(orderAdminService.getOrderPage(pageReqVO));
    }

    @GetMapping("/pending-ship/page")
    @Operation(summary = "搜索闲鱼待发货订单")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:ship')")
    public CommonResult<PageResult<XianyuPendingShipOrderRespVO>> getPendingShipOrderPage(
            @Valid XianyuPendingShipOrderPageReqVO pageReqVO) {
        return success(orderShipService.searchPendingOrders(pageReqVO));
    }

    @PostMapping("/ship/ocr")
    @Operation(summary = "识别发货图片中的运单号和快递公司")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:ship:ocr')")
    public CommonResult<XianyuShipmentOcrRespVO> ocrShipment(@RequestParam("file") MultipartFile file) {
        return success(shipmentOcrService.extract(file));
    }

    @PostMapping("/ship")
    @Operation(summary = "绑定设备并调用闲管家订单发货")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:ship')")
    public CommonResult<XianyuOrderShipRespVO> ship(@Valid @RequestBody XianyuOrderShipReqVO reqVO) {
        return success(orderShipService.ship(reqVO));
    }

    @PostMapping("/sync-page")
    @Operation(summary = "有界只读订单同步一页")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:sync')")
    public CommonResult<XianyuOrderSyncRespVO> syncPage(@Valid @RequestBody XianyuOrderSyncReqVO reqVO) {
        return success(orderAdminService.syncPage(reqVO));
    }

    @PostMapping("/reparse-remarks")
    @Operation(summary = "使用当前规则重新解析本租户历史订单备注")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:sync')")
    public CommonResult<Integer> reparseRemarks(
            @RequestParam(value = "maxOrders", defaultValue = "5000")
            @Min(1) @Max(10_000) Integer maxOrders) {
        return success(orderRemarkReparseService.reparse(maxOrders));
    }

    @PostMapping("/convert")
    @Operation(summary = "转换渠道订单为内部租赁订单")
    @PreAuthorize("@ss.hasPermission('rental:order:convert')")
    @Parameter(name = "channelOrderId", required = true)
    public CommonResult<RentalConversionResult> convert(@RequestParam("channelOrderId") Long channelOrderId) {
        return success(orderAdminService.convert(channelOrderId));
    }

}
