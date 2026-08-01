package cn.iocoder.yudao.module.rental.controller.admin.returnregistration;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationAdminService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminCreateResult;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminDetail;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminRow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/rental/return-registration")
@Validated
public class RentalReturnRegistrationController {

    private final ReturnRegistrationAdminService adminService;

    public RentalReturnRegistrationController(ReturnRegistrationAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('rental:return-registration:create')")
    public CommonResult<AdminCreateResult> create(@Valid @RequestBody CreateReq req) {
        return success(adminService.create(req.rentalOrderId(), req.validDays()));
    }

    @PostMapping("/{id}/reissue")
    @PreAuthorize("@ss.hasPermission('rental:return-registration:create')")
    public CommonResult<AdminCreateResult> reissue(
            @PathVariable Long id, @Valid @RequestBody ReissueReq req) {
        return success(adminService.reissue(id, req.validDays()));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('rental:return-registration:query')")
    public CommonResult<PageResult<AdminRow>> page(
            @Validated PageParam page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long rentalOrderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String serial,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedStart,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedEnd) {
        return success(adminService.page(page, status, rentalOrderId, keyword,
                serial, submittedStart, submittedEnd));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('rental:return-registration:query')")
    public CommonResult<AdminDetail> get(@RequestParam Long id) {
        return success(adminService.get(id));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("@ss.hasPermission('rental:return-registration:revoke')")
    public CommonResult<Boolean> revoke(@PathVariable Long id) {
        adminService.revoke(id);
        return success(true);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("@ss.hasPermission('rental:return-registration:review')")
    public CommonResult<Boolean> review(
            @PathVariable Long id, @Valid @RequestBody ReviewReq req) {
        adminService.review(id, req.accept(), req.note(), getLoginUserId());
        return success(true);
    }

    public record CreateReq(@NotNull Long rentalOrderId, @Min(1) @Max(30) Integer validDays) {
    }

    public record ReissueReq(@Min(1) @Max(30) Integer validDays) {
    }

    public record ReviewReq(boolean accept, @Size(max = 1000) String note) {
    }
}
