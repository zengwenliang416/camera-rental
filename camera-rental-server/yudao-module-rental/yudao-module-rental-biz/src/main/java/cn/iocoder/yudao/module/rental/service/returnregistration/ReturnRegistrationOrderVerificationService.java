package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_VERIFICATION_FAILED;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.PublicContext;

@Service
public class ReturnRegistrationOrderVerificationService {

    private final XianyuOrderMapper xianyuOrderMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalReturnRegistrationMapper registrationMapper;
    private final ReturnRegistrationTokenService tokenService;
    private final ReturnRegistrationPublicService publicService;
    private final ReturnRegistrationSecurityAuditService auditService;

    public ReturnRegistrationOrderVerificationService(
            XianyuOrderMapper xianyuOrderMapper,
            RentalOrderMapper orderMapper,
            RentalDeviceAssignmentMapper assignmentMapper,
            RentalReturnRegistrationMapper registrationMapper,
            ReturnRegistrationTokenService tokenService,
            ReturnRegistrationPublicService publicService,
            ReturnRegistrationSecurityAuditService auditService) {
        this.xianyuOrderMapper = xianyuOrderMapper;
        this.orderMapper = orderMapper;
        this.assignmentMapper = assignmentMapper;
        this.registrationMapper = registrationMapper;
        this.tokenService = tokenService;
        this.publicService = publicService;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public VerifiedSession verify(String rawOrderNo, String rawMobileLast4) {
        String orderNo = normalizeOrderNo(rawOrderNo);
        String mobileLast4 = normalizeMobileLast4(rawMobileLast4);
        List<XianyuOrderDO> candidates = StringUtils.hasText(orderNo)
                ? TenantUtils.executeIgnore(() -> xianyuOrderMapper.selectListByExternalOrderId(orderNo))
                : List.of();
        XianyuOrderDO candidate = candidates.size() == 1 ? candidates.get(0) : null;
        boolean mobileMatches = mobileMatches(
                candidate == null ? null : candidate.getReceiverMobile(), mobileLast4);
        if (candidate == null || !mobileMatches || candidate.getTenantId() == null
                || candidate.getRentalOrderId() == null) {
            auditService.recordVerification(orderNo, null, "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }
        return executeForTenant(candidate.getTenantId(),
                () -> verifyForTenant(candidate.getId(), orderNo, mobileLast4));
    }

    private VerifiedSession verifyForTenant(Long channelOrderId, String orderNo, String mobileLast4) {
        XianyuOrderDO channelOrder = xianyuOrderMapper.selectByIdForUpdate(channelOrderId);
        if (channelOrder == null || !orderNo.equals(channelOrder.getExternalOrderId())
                || !mobileMatches(channelOrder.getReceiverMobile(), mobileLast4)
                || channelOrder.getRentalOrderId() == null) {
            auditService.recordVerification(orderNo, null, "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }
        RentalOrderDO rentalOrder = orderMapper.selectByIdForUpdate(channelOrder.getRentalOrderId());
        if (rentalOrder == null
                || assignmentMapper.selectActiveListByRentalOrderId(rentalOrder.getId()).isEmpty()) {
            auditService.recordVerification(orderNo, null, "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }

        LocalDateTime now = LocalDateTime.now();
        RentalReturnRegistrationDO registration =
                registrationMapper.selectLatestByRentalOrderIdForUpdate(rentalOrder.getId());
        if (registration != null
                && ReturnRegistrationStatusEnum.REVOKED.name().equals(registration.getStatus())) {
            auditService.recordVerification(orderNo, registration.getId(), "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }
        if (registration == null
                || ReturnRegistrationStatusEnum.EXPIRED.name().equals(registration.getStatus())
                || (ReturnRegistrationStatusEnum.DRAFT.name().equals(registration.getStatus())
                && (registration.getExpiresAt() == null
                || !registration.getExpiresAt().isAfter(now)))) {
            if (registration != null
                    && ReturnRegistrationStatusEnum.DRAFT.name().equals(registration.getStatus())) {
                registration.setStatus(ReturnRegistrationStatusEnum.EXPIRED.name());
                registrationMapper.updateById(registration);
            }
            registration = new RentalReturnRegistrationDO()
                    .setFormNo("RR" + IdUtil.getSnowflakeNextIdStr())
                    .setRentalOrderId(rentalOrder.getId())
                    .setChannelOrderId(channelOrder.getId())
                    .setExternalOrderNo(channelOrder.getExternalOrderId())
                    .setStatus(ReturnRegistrationStatusEnum.DRAFT.name());
        }

        ReturnRegistrationTokenService.IssuedToken session = tokenService.issue();
        registration.setTokenHash(session.hash())
                .setExpiresAt(now.plus(ReturnRegistrationSessionCookieService.SESSION_DURATION))
                .setOpenedAt(now);
        if (registration.getId() == null) {
            registrationMapper.insert(registration);
        } else {
            registrationMapper.updateById(registration);
        }
        PublicContext context = publicService.getSessionContext(session.plaintext());
        auditService.recordVerification(orderNo, registration.getId(), "VERIFIED");
        return new VerifiedSession(session.plaintext(), registration.getId(), context);
    }

    static boolean mobileMatches(String receiverMobile, String mobileLast4) {
        String normalizedReceiver = digitsOnly(receiverMobile);
        String expected = normalizedReceiver.length() < 4
                ? "----" : normalizedReceiver.substring(normalizedReceiver.length() - 4);
        String provided = mobileLast4 != null && mobileLast4.matches("\\d{4}")
                ? mobileLast4 : "????";
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.getBytes(StandardCharsets.US_ASCII));
    }

    private static String normalizeOrderNo(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeMobileLast4(String value) {
        return digitsOnly(value);
    }

    private static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private <T> T executeForTenant(Long tenantId, Supplier<T> supplier) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        boolean previousIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(false);
            return supplier.get();
        } finally {
            TenantContextHolder.setTenantId(previousTenantId);
            TenantContextHolder.setIgnore(previousIgnore);
        }
    }

    public record VerifiedSession(String sessionToken, Long registrationId, PublicContext context) {
    }
}
