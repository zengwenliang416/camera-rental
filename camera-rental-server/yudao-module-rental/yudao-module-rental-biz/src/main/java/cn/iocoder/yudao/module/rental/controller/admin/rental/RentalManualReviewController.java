package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualReviewHandleReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualReviewRespVO;
import cn.iocoder.yudao.module.rental.service.admin.RentalManualReviewAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 人工复核")
@RestController
@RequestMapping("/rental/manual-review")
@Validated
public class RentalManualReviewController {

    private final RentalManualReviewAdminService reviewAdminService;

    public RentalManualReviewController(RentalManualReviewAdminService reviewAdminService) {
        this.reviewAdminService = reviewAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询人工复核")
    @PreAuthorize("@ss.hasPermission('rental:review:query')")
    public CommonResult<PageResult<RentalManualReviewRespVO>> getReviewPage(
            @RequestParam(value = "status", required = false) String status,
            @Validated PageParam pageParam) {
        return success(reviewAdminService.getReviewPage(status, pageParam));
    }

    @PutMapping("/resolve")
    @Operation(summary = "解决人工复核")
    @PreAuthorize("@ss.hasPermission('rental:review:update')")
    public CommonResult<Boolean> resolveReview(@Valid @RequestBody RentalManualReviewHandleReqVO reqVO) {
        reviewAdminService.resolveReview(reqVO.getId(), reqVO.getResolutionNote(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭人工复核")
    @PreAuthorize("@ss.hasPermission('rental:review:update')")
    public CommonResult<Boolean> closeReview(@Valid @RequestBody RentalManualReviewHandleReqVO reqVO) {
        reviewAdminService.closeReview(reqVO.getId(), reqVO.getResolutionNote(), getLoginUserId());
        return success(true);
    }

}
