package cn.iocoder.yudao.module.rental.controller.app.returnregistration;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.ratelimiter.core.annotation.RateLimiter;
import cn.iocoder.yudao.framework.ratelimiter.core.keyresolver.impl.ExpressionRateLimiterKeyResolver;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationAttachmentService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AttachmentView;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.PublicContext;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.Receipt;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.Submission;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.UploadAuthorization;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationPublicService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationResolver;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationSecurityAuditService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationSubmissionService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/rental/return-registration")
@Validated
@PermitAll
@TenantIgnore
public class AppReturnRegistrationController {

    private final ReturnRegistrationPublicService publicService;
    private final ReturnRegistrationAttachmentService attachmentService;
    private final ReturnRegistrationSubmissionService submissionService;
    private final ReturnRegistrationResolver resolver;
    private final ReturnRegistrationSecurityAuditService auditService;

    public AppReturnRegistrationController(ReturnRegistrationPublicService publicService,
                                           ReturnRegistrationAttachmentService attachmentService,
                                           ReturnRegistrationSubmissionService submissionService,
                                           ReturnRegistrationResolver resolver,
                                           ReturnRegistrationSecurityAuditService auditService) {
        this.publicService = publicService;
        this.attachmentService = attachmentService;
        this.submissionService = submissionService;
        this.resolver = resolver;
        this.auditService = auditService;
    }

    @GetMapping("/{token}")
    @RateLimiter(time = 1, timeUnit = TimeUnit.MINUTES, count = 120,
            keyResolver = ExpressionRateLimiterKeyResolver.class,
            keyArg = "T(cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationTokenService).rateLimitKey(#token)")
    public CommonResult<PublicContext> getContext(@PathVariable String token) {
        return success(publicService.getContext(token));
    }

    @PostMapping("/{token}/upload-authorizations")
    @RateLimiter(time = 1, timeUnit = TimeUnit.MINUTES, count = 30,
            keyResolver = ExpressionRateLimiterKeyResolver.class,
            keyArg = "T(cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationTokenService).rateLimitKey(#token)")
    public CommonResult<UploadAuthorization> authorize(
            @PathVariable String token, @Valid @RequestBody UploadReq req) {
        return success(attachmentService.authorize(
                token, req.category(), req.name(), req.contentType()));
    }

    @PostMapping("/{token}/attachments/confirm")
    @RateLimiter(time = 1, timeUnit = TimeUnit.MINUTES, count = 60,
            keyResolver = ExpressionRateLimiterKeyResolver.class,
            keyArg = "T(cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationTokenService).rateLimitKey(#token)")
    public CommonResult<AttachmentView> confirm(
            @PathVariable String token, @Valid @RequestBody ConfirmReq req) {
        AttachmentView result = attachmentService.confirm(token, req.attachmentId());
        auditService.record("UPLOAD_CONFIRM", token, resolver.require(token).getId(), "CONFIRMED");
        return success(result);
    }

    @DeleteMapping("/{token}/attachments/{attachmentId}")
    @RateLimiter(time = 1, timeUnit = TimeUnit.MINUTES, count = 60,
            keyResolver = ExpressionRateLimiterKeyResolver.class,
            keyArg = "T(cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationTokenService).rateLimitKey(#token)")
    public CommonResult<Boolean> remove(
            @PathVariable String token, @PathVariable Long attachmentId) {
        attachmentService.remove(token, attachmentId);
        return success(true);
    }

    @PostMapping("/{token}/submit")
    @RateLimiter(time = 1, timeUnit = TimeUnit.MINUTES, count = 10,
            keyResolver = ExpressionRateLimiterKeyResolver.class,
            keyArg = "T(cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationTokenService).rateLimitKey(#token)")
    public CommonResult<Receipt> submit(
            @PathVariable String token, @Valid @RequestBody SubmitReq req) {
        Receipt result = submissionService.submit(token, new Submission(
                req.orderNo(), req.carrierCode(), req.carrierName(), req.waybillNo(),
                req.shippedDate(), req.serials(), req.attachmentIds(),
                req.issueDescription(), req.idempotencyKey()));
        auditService.record("SUBMIT", token, resolver.require(token).getId(), result.status());
        return success(result);
    }

    public record UploadReq(
            @NotBlank String category,
            @NotBlank @Size(max = 255) String name,
            @NotBlank String contentType
    ) {
    }

    public record ConfirmReq(@NotNull Long attachmentId) {
    }

    public record SubmitReq(
            @NotBlank String orderNo,
            @NotBlank String carrierCode,
            @NotBlank String carrierName,
            @NotBlank String waybillNo,
            @NotNull LocalDate shippedDate,
            @NotEmpty @Size(max = 8) List<String> serials,
            @NotEmpty @Size(max = 20) List<Long> attachmentIds,
            @Size(max = 1000) String issueDescription,
            @NotBlank @Size(max = 128) String idempotencyKey
    ) {
    }

}
