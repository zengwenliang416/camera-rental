package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_VERIFICATION_FAILED;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.PublicContext;

@Service
public class ReturnRegistrationOrderVerificationService {

    private final XianyuOrderMapper xianyuOrderMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalReturnRegistrationMapper registrationMapper;
    private final ReturnRegistrationTokenService tokenService;
    private final ReturnRegistrationPublicService publicService;
    private final ReturnRegistrationSecurityAuditService auditService;
    private final ReturnSerialNormalizer serialNormalizer;
    private final Long publicTenantId;

    public ReturnRegistrationOrderVerificationService(
            XianyuOrderMapper xianyuOrderMapper,
            RentalOrderMapper orderMapper,
            RentalReturnRegistrationMapper registrationMapper,
            ReturnRegistrationTokenService tokenService,
            ReturnRegistrationPublicService publicService,
            ReturnRegistrationSecurityAuditService auditService,
            ReturnSerialNormalizer serialNormalizer,
            @Value("${yudao.rental.return-registration.public-tenant-id:1}") Long publicTenantId) {
        this.xianyuOrderMapper = xianyuOrderMapper;
        this.orderMapper = orderMapper;
        this.registrationMapper = registrationMapper;
        this.tokenService = tokenService;
        this.publicService = publicService;
        this.auditService = auditService;
        this.serialNormalizer = serialNormalizer;
        this.publicTenantId = publicTenantId;
    }

    @Transactional(rollbackFor = Exception.class)
    public VerifiedSession verify(String rawOrderNo, String rawMobileLast4, String rawMachineCode) {
        String orderNo = normalizeOrderNo(rawOrderNo);
        String mobileLast4 = normalizeMobileLast4(rawMobileLast4);
        String machineCode = serialNormalizer.normalize(rawMachineCode);
        boolean mobileProvided = StringUtils.hasText(rawMobileLast4);
        if ((mobileProvided && !mobileLast4.matches("\\d{4}"))
                || !serialNormalizer.isValid(machineCode)) {
            auditService.recordVerification(orderNo, mobileLast4, machineCode, null, "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }
        List<XianyuOrderDO> candidates = findCandidates(orderNo, mobileLast4, machineCode).stream()
                .filter(candidate -> Objects.equals(publicTenantId, candidate.getTenantId()))
                .filter(candidate -> mobileRequirementSatisfied(candidate, orderNo, mobileLast4))
                .limit(2)
                .toList();
        XianyuOrderDO candidate = candidates.size() == 1 ? candidates.get(0) : null;
        if (candidate == null && candidates.isEmpty()
                && !StringUtils.hasText(orderNo) && !StringUtils.hasText(mobileLast4)) {
            return createStandaloneReviewDraft(orderNo, mobileLast4, machineCode);
        }
        if (candidate == null || candidate.getTenantId() == null) {
            auditService.recordVerification(orderNo, mobileLast4, machineCode, null, "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }
        return executeForTenant(candidate.getTenantId(),
                () -> verifyForTenant(candidate.getId(), orderNo, mobileLast4, machineCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public VerifiedSession verifyOrReuseStandalone(String sessionToken, String rawOrderNo,
                                                   String rawMobileLast4, String rawMachineCode) {
        if (!StringUtils.hasText(rawOrderNo) && !StringUtils.hasText(rawMobileLast4)
                && StringUtils.hasText(sessionToken)) {
            RentalReturnRegistrationDO existing = TenantUtils.executeIgnore(
                    () -> registrationMapper.selectByTokenHash(tokenService.hash(sessionToken)));
            if (existing != null
                    && Objects.equals(publicTenantId, existing.getTenantId())
                    && existing.getRentalOrderId() == null
                    && existing.getChannelOrderId() == null
                    && !StringUtils.hasText(existing.getExternalOrderNo())
                    && ReturnRegistrationStatusEnum.DRAFT.name().equals(existing.getStatus())
                    && existing.getExpiresAt() != null
                    && existing.getExpiresAt().isAfter(LocalDateTime.now())) {
                return new VerifiedSession(sessionToken, existing.getId(),
                        publicService.getSessionContext(sessionToken));
            }
        }
        return verify(rawOrderNo, rawMobileLast4, rawMachineCode);
    }

    private List<XianyuOrderDO> findCandidates(String orderNo, String mobileLast4, String machineCode) {
        if (StringUtils.hasText(orderNo)) {
            return TenantUtils.executeIgnore(
                    () -> xianyuOrderMapper.selectListByExternalOrderId(orderNo));
        }
        if (mobileLast4.matches("\\d{4}")) {
            return TenantUtils.executeIgnore(
                    () -> xianyuOrderMapper.selectListByReceiverMobileLast4(mobileLast4));
        }
        return TenantUtils.executeIgnore(
                () -> xianyuOrderMapper.selectListByAssignedMachineCode(machineCode));
    }

    private VerifiedSession verifyForTenant(Long channelOrderId, String orderNo, String mobileLast4,
                                            String machineCode) {
        XianyuOrderDO channelOrder = xianyuOrderMapper.selectByIdForUpdate(channelOrderId);
        if (channelOrder == null
                || (StringUtils.hasText(orderNo)
                && !orderNo.equals(channelOrder.getExternalOrderId()))
                || !mobileRequirementSatisfied(channelOrder, orderNo, mobileLast4)) {
            auditService.recordVerification(orderNo, mobileLast4, machineCode, null, "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }
        RentalOrderDO rentalOrder = channelOrder.getRentalOrderId() == null
                ? null : orderMapper.selectByIdForUpdate(channelOrder.getRentalOrderId());
        if (channelOrder.getRentalOrderId() != null && rentalOrder == null) {
            auditService.recordVerification(orderNo, mobileLast4, machineCode, null, "REJECTED");
            throw exception(RETURN_REGISTRATION_VERIFICATION_FAILED);
        }

        LocalDateTime now = LocalDateTime.now();
        RentalReturnRegistrationDO registration =
                registrationMapper.selectLatestByChannelOrderIdForUpdate(channelOrder.getId());
        if (registration != null
                && ReturnRegistrationStatusEnum.REVOKED.name().equals(registration.getStatus())) {
            auditService.recordVerification(
                    orderNo, mobileLast4, machineCode, registration.getId(), "REJECTED");
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
                    .setRentalOrderId(rentalOrder == null ? null : rentalOrder.getId())
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
        auditService.recordVerification(
                orderNo, mobileLast4, machineCode, registration.getId(), "VERIFIED");
        return new VerifiedSession(session.plaintext(), registration.getId(), context);
    }

    private VerifiedSession createStandaloneReviewDraft(String orderNo, String mobileLast4,
                                                        String machineCode) {
        return executeForTenant(publicTenantId, () -> {
            LocalDateTime now = LocalDateTime.now();
            ReturnRegistrationTokenService.IssuedToken session = tokenService.issue();
            RentalReturnRegistrationDO registration = new RentalReturnRegistrationDO()
                    .setFormNo("RR" + IdUtil.getSnowflakeNextIdStr())
                    .setRentalOrderId(null)
                    .setChannelOrderId(null)
                    .setExternalOrderNo("")
                    .setTokenHash(session.hash())
                    .setStatus(ReturnRegistrationStatusEnum.DRAFT.name())
                    .setExpiresAt(now.plus(ReturnRegistrationSessionCookieService.SESSION_DURATION))
                    .setOpenedAt(now);
            registration.setTenantId(publicTenantId);
            registrationMapper.insert(registration);
            PublicContext context = publicService.getSessionContext(session.plaintext());
            auditService.recordVerification(
                    orderNo, mobileLast4, machineCode, registration.getId(), "STANDALONE_REVIEW");
            return new VerifiedSession(session.plaintext(), registration.getId(), context);
        });
    }

    private static boolean mobileRequirementSatisfied(XianyuOrderDO candidate, String orderNo,
                                                       String mobileLast4) {
        if (!StringUtils.hasText(mobileLast4)) {
            return true;
        }
        if (!StringUtils.hasText(candidate.getReceiverMobile()) && StringUtils.hasText(orderNo)) {
            return true;
        }
        return mobileMatches(candidate.getReceiverMobile(), mobileLast4);
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
