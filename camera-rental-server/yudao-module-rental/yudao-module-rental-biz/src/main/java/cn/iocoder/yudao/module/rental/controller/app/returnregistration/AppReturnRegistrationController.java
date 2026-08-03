package cn.iocoder.yudao.module.rental.controller.app.returnregistration;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationAttachmentService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AttachmentView;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.PublicContext;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.Receipt;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.Submission;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.UploadAuthorization;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationOrderVerificationService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationPublicService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationRateLimitService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationResolver;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationSecurityAuditService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationSessionCookieService;
import cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationSubmissionService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_NOT_AVAILABLE;

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
    private final ReturnRegistrationOrderVerificationService verificationService;
    private final ReturnRegistrationSessionCookieService cookieService;
    private final ReturnRegistrationRateLimitService rateLimitService;

    public AppReturnRegistrationController(ReturnRegistrationPublicService publicService,
                                           ReturnRegistrationAttachmentService attachmentService,
                                           ReturnRegistrationSubmissionService submissionService,
                                           ReturnRegistrationResolver resolver,
                                           ReturnRegistrationSecurityAuditService auditService,
                                           ReturnRegistrationOrderVerificationService verificationService,
                                           ReturnRegistrationSessionCookieService cookieService,
                                           ReturnRegistrationRateLimitService rateLimitService) {
        this.publicService = publicService;
        this.attachmentService = attachmentService;
        this.submissionService = submissionService;
        this.resolver = resolver;
        this.auditService = auditService;
        this.verificationService = verificationService;
        this.cookieService = cookieService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/verify")
    @PermitAll
    public CommonResult<PublicContext> verify(
            @Valid @RequestBody VerifyReq req,
            HttpServletRequest request,
            HttpServletResponse response) {
        rateLimitService.checkVerification(
                ServletUtils.getClientIP(request), req.orderNo(), req.mobileLast4(), req.machineCode());
        ReturnRegistrationOrderVerificationService.VerifiedSession verified =
                verificationService.verify(req.orderNo(), req.mobileLast4(), req.machineCode());
        cookieService.write(response, verified.sessionToken());
        return success(verified.context());
    }

    @GetMapping("/session")
    @PermitAll
    public CommonResult<PublicContext> getSessionContext(
            @CookieValue(name = ReturnRegistrationSessionCookieService.COOKIE_NAME,
                    required = false) String sessionToken) {
        String session = requireSessionCookie(sessionToken);
        rateLimitService.checkSession(session, "context", 120);
        return success(publicService.getSessionContext(session));
    }

    @PostMapping("/upload-authorizations")
    @PermitAll
    public CommonResult<UploadAuthorization> authorizeSession(
            @CookieValue(name = ReturnRegistrationSessionCookieService.COOKIE_NAME,
                    required = false) String sessionToken,
            @Valid @RequestBody UploadReq req) {
        String session = requireSessionCookie(sessionToken);
        rateLimitService.checkSession(session, "authorize", 30);
        return success(attachmentService.authorize(
                session, req.category(), req.name(), req.contentType()));
    }

    @PostMapping("/attachments/confirm")
    @PermitAll
    public CommonResult<AttachmentView> confirmSession(
            @CookieValue(name = ReturnRegistrationSessionCookieService.COOKIE_NAME,
                    required = false) String sessionToken,
            @Valid @RequestBody ConfirmReq req) {
        String session = requireSessionCookie(sessionToken);
        rateLimitService.checkSession(session, "confirm", 60);
        AttachmentView result = attachmentService.confirm(session, req.attachmentId());
        auditService.record("UPLOAD_CONFIRM", session,
                resolver.requireSession(session).getId(), "CONFIRMED");
        return success(result);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PermitAll
    public CommonResult<Boolean> removeSession(
            @CookieValue(name = ReturnRegistrationSessionCookieService.COOKIE_NAME,
                    required = false) String sessionToken,
            @PathVariable Long attachmentId) {
        String session = requireSessionCookie(sessionToken);
        rateLimitService.checkSession(session, "remove", 60);
        attachmentService.remove(session, attachmentId);
        return success(true);
    }

    @PostMapping("/submit")
    @PermitAll
    public CommonResult<Receipt> submitSession(
            @CookieValue(name = ReturnRegistrationSessionCookieService.COOKIE_NAME,
                    required = false) String sessionToken,
            @Valid @RequestBody SubmitReq req) {
        String session = requireSessionCookie(sessionToken);
        rateLimitService.checkSession(session, "submit", 10);
        Receipt result = submissionService.submit(session, new Submission(
                req.orderNo(), req.carrierCode(), req.carrierName(), req.waybillNo(),
                req.shippedDate(), req.serials(), req.attachmentIds(),
                req.issueDescription(), req.idempotencyKey()));
        auditService.record("SUBMIT", session,
                resolver.requireSession(session).getId(), result.status());
        return success(result);
    }

    @PostMapping("/simple-submit")
    @PermitAll
    public CommonResult<Receipt> submitSimple(
            @Valid @RequestBody SimpleSubmitReq req,
            @CookieValue(name = ReturnRegistrationSessionCookieService.COOKIE_NAME,
                    required = false) String sessionToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        rateLimitService.checkVerification(
                ServletUtils.getClientIP(request), req.orderNo(), req.mobileLast4(), req.machineCode());
        ReturnRegistrationOrderVerificationService.VerifiedSession verified =
                verificationService.verifyOrReuseStandalone(
                        sessionToken, req.orderNo(), req.mobileLast4(), req.machineCode());
        cookieService.write(response, verified.sessionToken());
        rateLimitService.checkSession(verified.sessionToken(), "simple-submit", 10);
        Receipt result = submissionService.submitSimple(
                verified.sessionToken(), req.machineCode(), req.waybillNo(),
                req.attachmentIds());
        auditService.record("SIMPLE_SUBMIT", verified.sessionToken(),
                verified.registrationId(), result.status());
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

    public record VerifyReq(
            @Size(max = 128) String orderNo,
            @Size(max = 32) String mobileLast4,
            @NotBlank @Size(max = 128) String machineCode
    ) {
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

    public record SimpleSubmitReq(
            @Size(max = 128) String orderNo,
            @Size(max = 32) String mobileLast4,
            @NotBlank @Size(max = 128) String machineCode,
            @NotBlank @Size(max = 128) String waybillNo,
            @Size(max = 10) List<Long> attachmentIds
    ) {
    }

    private String requireSessionCookie(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw exception(RETURN_REGISTRATION_NOT_AVAILABLE);
        }
        resolver.requireSession(sessionToken);
        return sessionToken;
    }

}
