package cn.iocoder.yudao.module.rental.controller.admin.rental;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffWarehouseVO.*;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.service.admin.*;
import jakarta.validation.Valid;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/rental/staff")
@RequiredArgsConstructor
@Validated
public class RentalStaffWarehouseController {
    private final RentalScheduleAllocationService allocation;
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/available-devices") @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCandidatesRespVO> available(
            @RequestParam @NotBlank @Size(max=64) String modelCode,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endExclusive) {
        return success(allocation.previewCandidates(modelCode, from, endExclusive));
    }
    private final RentalStaffTaskService tasks;
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/tasks") @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<PageResult<cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffTaskRespVO>> tasks(
            @Valid PageParam page, @RequestParam @Pattern(regexp="SHIP|RETURN|OVERDUE|INSPECT|REPAIR") String queue) {
        return success(tasks.page(page, queue));
    }
    private final RentalStaffIssueService issues;
    private final RentalStaffStocktakeService stocktakes;
    private final RentalStaffInspectionService inspections;

    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/issues") @PreAuthorize("@ss.hasPermission('rental:schedule:query')")
    public CommonResult<PageResult<Issue>> issues(@Valid PageParam page,
            @RequestParam(required=false) @Pattern(regexp="OPEN|PROCESSING|RESOLVED") String status) { return success(issues.page(page,status)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/issues") @PreAuthorize("@ss.hasAnyPermissions('rental:device:assign','rental:xianyu:ship')")
    public CommonResult<Long> issue(@Valid @RequestBody IssueSave request) { return success(issues.create(request)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/issues/action") @PreAuthorize("@ss.hasAnyPermissions('rental:device:assign','rental:xianyu:ship')")
    public CommonResult<Boolean> action(@Valid @RequestBody IssueAction request) { issues.action(request); return success(true); }

    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/stocktakes") @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<PageResult<Stocktake>> stocktakes(@Valid PageParam page) { return success(stocktakes.page(page)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/stocktakes/{id}") @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<Stocktake> stocktake(@PathVariable Long id) { return success(stocktakes.get(id)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/stocktakes") @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<Long> createStocktake(@Valid @RequestBody StocktakeCreate request) { return success(stocktakes.create(request)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/stocktakes/scan") @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<Boolean> scan(@Valid @RequestBody StocktakeAction request) { stocktakes.scan(request); return success(true); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/stocktakes/{id}/close") @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<Boolean> close(@PathVariable Long id) { stocktakes.close(id); return success(true); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/stocktakes/move") @PreAuthorize("@ss.hasPermission('rental:device:update')")
    public CommonResult<Boolean> move(@Valid @RequestBody StocktakeAction request) { stocktakes.move(request); return success(true); }

    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/inspection/template") @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<List<Check>> template(@RequestParam @Size(max=64) String modelCode) { return success(inspections.template(modelCode)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PutMapping("/inspection/template") @PreAuthorize("@ss.hasPermission('rental:device:update')")
    public CommonResult<Boolean> template(@Valid @RequestBody TemplateSave request) { inspections.saveTemplate(request); return success(true); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/inspection") @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<RentalDeviceOpsRespVO> inspection(@Valid @RequestBody InspectionSubmit request) { return success(inspections.submit(request)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/inspection/history") @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<List<Inspection>> history(@RequestParam Long deviceId) { return success(inspections.history(deviceId)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/photos") @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<PhotoUpload> photo(@Valid @RequestBody PhotoAuthorize request) { return success(inspections.authorize(request)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @GetMapping("/photos/{id}") @PreAuthorize("@ss.hasPermission('rental:device:query')")
    public CommonResult<Photo> photoView(@PathVariable Long id) { return success(inspections.photo(id)); }
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PostMapping("/photos/{id}/confirm") @PreAuthorize("@ss.hasPermission('rental:device:assign')")
    public CommonResult<Photo> photoConfirm(@PathVariable Long id) { return success(inspections.confirm(id)); }
}
