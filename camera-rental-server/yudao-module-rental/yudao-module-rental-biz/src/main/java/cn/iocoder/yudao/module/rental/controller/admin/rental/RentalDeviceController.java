package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceAssignReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCategoryCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCategoryRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceGenerateFromPurchaseReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceGenerateFromPurchaseRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceQrRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceResolveQrReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDevicePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceReturnReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceUnassignReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceUpdateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceModelCreateReqVO;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceAdminService;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceInboundService;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceOpsService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 租赁设备")
@RestController
@RequestMapping("/rental/device")
@Validated
public class RentalDeviceController {

    private final RentalDeviceAdminService deviceAdminService;
    private final RentalDeviceOpsService deviceOpsService;
    private final RentalDeviceInboundService deviceInboundService;
    private final RentalDeviceCatalogService deviceCatalogService;

    public RentalDeviceController(RentalDeviceAdminService deviceAdminService,
                                  RentalDeviceOpsService deviceOpsService,
                                  RentalDeviceInboundService deviceInboundService,
                                  RentalDeviceCatalogService deviceCatalogService) {
        this.deviceAdminService = deviceAdminService;
        this.deviceOpsService = deviceOpsService;
        this.deviceInboundService = deviceInboundService;
        this.deviceCatalogService = deviceCatalogService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询设备")
    @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<PageResult<RentalDeviceRespVO>> getDevicePage(
            @Valid RentalDevicePageReqVO reqVO) {
        return success(deviceAdminService.getDevicePage(reqVO));
    }

    @GetMapping("/catalog")
    @Operation(summary = "获取设备大类和型号目录")
    @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<List<RentalDeviceCategoryRespVO>> getDeviceCatalog() {
        return success(deviceCatalogService.getCatalog());
    }

    @PostMapping("/catalog/category/create")
    @Operation(summary = "新增当前租户设备大类")
    @PreAuthorize("@ss.hasPermission('rental:device:create')")
    public CommonResult<Long> createDeviceCategory(
            @Valid @RequestBody RentalDeviceCategoryCreateReqVO reqVO) {
        return success(deviceCatalogService.createCategory(reqVO));
    }

    @PostMapping("/catalog/model/create")
    @Operation(summary = "新增当前租户设备型号和编号前缀")
    @PreAuthorize("@ss.hasPermission('rental:device:create')")
    public CommonResult<Long> createDeviceModel(
            @Valid @RequestBody RentalDeviceModelCreateReqVO reqVO) {
        return success(deviceCatalogService.createModel(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建设备实例")
    @PreAuthorize("@ss.hasPermission('rental:device:create')")
    public CommonResult<Long> createDevice(@Valid @RequestBody RentalDeviceCreateReqVO reqVO) {
        return success(deviceAdminService.createDevice(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备可变信息")
    @PreAuthorize("@ss.hasPermission('rental:device:update')")
    public CommonResult<Boolean> updateDevice(@Valid @RequestBody RentalDeviceUpdateReqVO reqVO) {
        deviceAdminService.updateDevice(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "逻辑删除无业务引用的设备")
    @PreAuthorize("@ss.hasPermission('rental:device:delete')")
    public CommonResult<Boolean> deleteDevice(@RequestParam("id") Long id) {
        deviceAdminService.deleteDevice(id);
        return success(true);
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

    @GetMapping("/get-qr")
    @Operation(summary = "获取设备永久二维码载荷（署名型号）")
    @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<RentalDeviceQrRespVO> getDeviceQr(@RequestParam("id") Long id) {
        return success(deviceAdminService.getDeviceQr(id));
    }

    @PostMapping("/resolve-qr")
    @Operation(summary = "解析设备永久二维码")
    @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<RentalDeviceRespVO> resolveDeviceQr(@Valid @RequestBody RentalDeviceResolveQrReqVO reqVO) {
        return success(deviceAdminService.resolveDeviceQr(reqVO.getPayload()));
    }

    @PostMapping("/dispatch")
    @Operation(summary = "设备出库（扫码/按分配出库）")
    @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<RentalDeviceOpsRespVO> dispatch(@Valid @RequestBody RentalDeviceDispatchReqVO reqVO) {
        return success(deviceOpsService.dispatch(reqVO));
    }

    @PostMapping("/return")
    @Operation(summary = "设备回仓（含检测通过/不通过）")
    @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<RentalDeviceOpsRespVO> returnDevice(@Valid @RequestBody RentalDeviceReturnReqVO reqVO) {
        return success(deviceOpsService.returnDevice(reqVO));
    }

    @PostMapping("/unassign")
    @Operation(summary = "撤销设备分配（仅未出库，联动取消占用排期）")
    @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<RentalDeviceOpsRespVO> unassign(@Valid @RequestBody RentalDeviceUnassignReqVO reqVO) {
        return success(deviceOpsService.unassign(reqVO));
    }

    @PostMapping("/generate-from-purchase-in")
    @Operation(summary = "从已审批采购入库单生成租赁设备实例")
    @PreAuthorize("@ss.hasPermission('rental:device:create')")
    public CommonResult<RentalDeviceGenerateFromPurchaseRespVO> generateFromPurchaseIn(
            @Valid @RequestBody RentalDeviceGenerateFromPurchaseReqVO reqVO) {
        return success(deviceInboundService.generateFromPurchaseIn(reqVO));
    }

}
