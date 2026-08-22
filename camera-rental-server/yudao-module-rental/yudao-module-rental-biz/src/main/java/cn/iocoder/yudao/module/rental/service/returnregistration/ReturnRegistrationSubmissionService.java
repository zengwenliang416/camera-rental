package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryDirectionEnum;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnDeviceMatchStatusEnum;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnMethodEnum;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryCreateCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryDeviceCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryService;
import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_ORDER_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_STATUS_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_SUBMISSION_INVALID;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.Receipt;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.Submission;

@Service
public class ReturnRegistrationSubmissionService {

    private final ReturnRegistrationResolver resolver;
    private final ReturnSerialNormalizer serialNormalizer;
    private final ReturnRegistrationAttachmentService attachmentService;
    private final RentalReturnRegistrationMapper registrationMapper;
    private final RentalReturnRegistrationDeviceMapper registrationDeviceMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalDeliveryService deliveryService;

    public ReturnRegistrationSubmissionService(
            ReturnRegistrationResolver resolver,
            ReturnSerialNormalizer serialNormalizer,
            ReturnRegistrationAttachmentService attachmentService,
            RentalReturnRegistrationMapper registrationMapper,
            RentalReturnRegistrationDeviceMapper registrationDeviceMapper,
            RentalDeviceAssignmentMapper assignmentMapper,
            RentalDeviceMapper deviceMapper,
            RentalDeliveryService deliveryService) {
        this.resolver = resolver;
        this.serialNormalizer = serialNormalizer;
        this.attachmentService = attachmentService;
        this.registrationMapper = registrationMapper;
        this.registrationDeviceMapper = registrationDeviceMapper;
        this.assignmentMapper = assignmentMapper;
        this.deviceMapper = deviceMapper;
        this.deliveryService = deliveryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Receipt submit(String token, Submission submission) {
        RentalReturnRegistrationDO resolved = resolver.require(token);
        return resolver.execute(resolved, () -> submitLocked(resolved.getId(), submission, false));
    }

    @Transactional(rollbackFor = Exception.class)
    public Receipt submitSimple(String token, String machineCode, String waybillNo,
                                String returnMethod, List<Long> attachmentIds) {
        RentalReturnRegistrationDO resolved = resolver.require(token);
        return resolver.execute(resolved, () -> {
            ReturnMethodEnum method = resolveMethod(returnMethod);
            String effectiveWaybill = method == ReturnMethodEnum.SELF_DELIVERY ? null : waybillNo;
            String normalizedWaybill = normalizeWaybill(effectiveWaybill);
            Submission submission = new Submission(
                    resolved.getExternalOrderNo(),
                    simpleCarrierCode(method),
                    simpleCarrierName(method),
                    effectiveWaybill,
                    method.name(),
                    LocalDate.now(ZoneId.of("Asia/Shanghai")),
                    List.of(machineCode),
                    attachmentIds == null ? List.of() : attachmentIds,
                    null,
                    "simple:" + DigestUtil.sha256Hex(resolved.getId() + ":" + normalizedWaybill));
            return submitLocked(resolved.getId(), submission, true);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public Receipt acceptReview(Long registrationId) {
        RentalReturnRegistrationDO registration = registrationMapper.selectByIdForUpdate(registrationId);
        if (registration == null
                || !ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name().equals(registration.getStatus())) {
            throw exception(RETURN_REGISTRATION_STATUS_INVALID);
        }
        List<String> serials = registrationDeviceMapper.selectListByRegistrationId(registrationId)
                .stream().map(RentalReturnRegistrationDeviceDO::getSubmittedSerial).toList();
        MatchResult match = replaceDeviceMatches(registration, serials);
        if (!match.safe()) {
            throw exception(RETURN_REGISTRATION_SUBMISSION_INVALID, "设备仍无法与订单安全匹配");
        }
        Long deliveryId = createDelivery(registration, match.devices());
        registration.setStatus(ReturnRegistrationStatusEnum.ACCEPTED.name())
                .setDeliveryId(deliveryId)
                .setReviewedAt(LocalDateTime.now());
        registrationMapper.updateById(registration);
        return receipt(registration);
    }

    private Receipt submitLocked(Long registrationId, Submission submission,
                                 boolean validateOptionalAttachments) {
        RentalReturnRegistrationDO registration = registrationMapper.selectByIdForUpdate(registrationId);
        if (registration == null) {
            throw exception(RETURN_REGISTRATION_STATUS_INVALID);
        }
        if (!ReturnRegistrationStatusEnum.DRAFT.name().equals(registration.getStatus())) {
            return receipt(registration);
        }
        if (!ReturnRegistrationStatusEnum.DRAFT.name().equals(resolver.publicStatus(registration))) {
            throw exception(RETURN_REGISTRATION_STATUS_INVALID);
        }
        validateSubmission(registration, submission);
        if (validateOptionalAttachments) {
            attachmentService.validateOptionalForSubmission(
                    registration, submission.attachmentIds());
        }
        MatchResult match = replaceDeviceMatches(registration, submission.serials());
        String waybillNo = trimToNull(submission.waybillNo());

        registration.setReturnMethod(resolveMethod(submission.returnMethod()).name())
                .setCarrierCode(normalizeCode(submission.carrierCode()))
                .setCarrierName(submission.carrierName().trim())
                .setWaybillNo(waybillNo)
                .setNormalizedWaybillNo(waybillNo == null ? null : normalizeWaybill(waybillNo))
                .setShippedDate(submission.shippedDate())
                .setIssueDescription(trimToNull(submission.issueDescription()))
                .setIdempotencyKey(submission.idempotencyKey().trim())
                .setSubmittedAt(LocalDateTime.now());
        boolean needsReview = !match.safe() || StringUtils.hasText(registration.getIssueDescription());
        if (needsReview) {
            registration.setStatus(ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name());
        } else {
            registration.setDeliveryId(createDelivery(registration, match.devices()))
                    .setStatus(ReturnRegistrationStatusEnum.ACCEPTED.name());
        }
        registrationMapper.updateById(registration);
        return receipt(registration);
    }

    private MatchResult replaceDeviceMatches(RentalReturnRegistrationDO registration, List<String> serials) {
        registrationDeviceMapper.deleteByRegistrationId(registration.getId());
        List<RentalDeviceAssignmentDO> assignments = registration.getRentalOrderId() == null
                ? List.of()
                : assignmentMapper.selectActiveListByRentalOrderId(registration.getRentalOrderId());
        Map<String, MatchedDevice> allowed = new HashMap<>();
        for (RentalDeviceAssignmentDO assignment : assignments) {
            RentalDeviceDO device = deviceMapper.selectById(assignment.getDeviceId());
            if (device == null) {
                continue;
            }
            addCandidate(allowed, device.getSerialNumber(), assignment, device);
            addCandidate(allowed, device.getLegacyDeviceNo(), assignment, device);
            addCandidate(allowed, device.getDeviceNo(), assignment, device);
        }
        Set<String> seen = new HashSet<>();
        List<MatchedDevice> matched = new ArrayList<>();
        boolean safe = true;
        for (int index = 0; index < serials.size(); index++) {
            String submitted = serials.get(index);
            String normalized = serialNormalizer.normalize(submitted);
            ReturnDeviceMatchStatusEnum status;
            String message = null;
            MatchedDevice found = allowed.get(normalized);
            if (!seen.add(normalized)) {
                status = ReturnDeviceMatchStatusEnum.DUPLICATE;
                message = "同一次登记内序列号重复";
            } else if (found == null) {
                status = ReturnDeviceMatchStatusEnum.NOT_ASSIGNED;
                message = "未匹配到当前订单已分配设备";
            } else {
                status = ReturnDeviceMatchStatusEnum.MATCHED;
                matched.add(found);
            }
            safe &= status == ReturnDeviceMatchStatusEnum.MATCHED;
            RentalReturnRegistrationDeviceDO row = new RentalReturnRegistrationDeviceDO()
                    .setRegistrationId(registration.getId())
                    .setDeviceId(found == null ? null : found.device().getId())
                    .setAssignmentId(found == null ? null : found.assignment().getId())
                    .setSubmittedSerial(submitted.trim())
                    .setNormalizedSerial(normalized)
                    .setMatchStatus(status.name())
                    .setMatchMessage(message)
                    .setSortNo(index);
            registrationDeviceMapper.insert(row);
        }
        return new MatchResult(safe && matched.size() == serials.size(), matched);
    }

    private void validateSubmission(RentalReturnRegistrationDO registration, Submission submission) {
        if (submission == null || !Objects.equals(registration.getExternalOrderNo(), submission.orderNo())
                || !StringUtils.hasText(submission.carrierCode())
                || !StringUtils.hasText(submission.carrierName())
                || submission.shippedDate() == null
                || !StringUtils.hasText(submission.idempotencyKey())) {
            throw exception(RETURN_REGISTRATION_ORDER_INVALID);
        }
        if (resolveMethod(submission.returnMethod()) == ReturnMethodEnum.EXPRESS
                && !StringUtils.hasText(submission.waybillNo())) {
            throw exception(RETURN_REGISTRATION_ORDER_INVALID);
        }
        if (submission.serials() == null || submission.serials().isEmpty()
                || submission.serials().size() > 8
                || submission.serials().stream().anyMatch(value -> !serialNormalizer.isValid(value))) {
            throw exception(RETURN_REGISTRATION_SUBMISSION_INVALID,
                    "设备机器编码应为 1-8 个，格式类似 P4-01");
        }
    }

    private ReturnMethodEnum resolveMethod(String returnMethod) {
        if (!StringUtils.hasText(returnMethod)) {
            return ReturnMethodEnum.EXPRESS;
        }
        try {
            return ReturnMethodEnum.valueOf(returnMethod.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException cause) {
            throw exception(RETURN_REGISTRATION_SUBMISSION_INVALID, "不支持的归还方式");
        }
    }

    private String simpleCarrierCode(ReturnMethodEnum method) {
        return switch (method) {
            case EXPRESS -> "UNSPECIFIED";
            case SELF_DELIVERY -> "SELF_DELIVERY";
            case ERRAND -> "ERRAND";
        };
    }

    private String simpleCarrierName(ReturnMethodEnum method) {
        return switch (method) {
            case EXPRESS -> "待仓库识别";
            case SELF_DELIVERY -> "本人送回";
            case ERRAND -> "跑腿送回";
        };
    }

    private Long createDelivery(RentalReturnRegistrationDO registration, List<MatchedDevice> devices) {
        List<RentalDeliveryDeviceCommand> commands = devices.stream()
                .map(value -> new RentalDeliveryDeviceCommand(
                        value.assignment().getRentalOrderItemId(),
                        value.assignment().getId(),
                        value.device().getId()))
                .toList();
        String waybillNo = StringUtils.hasText(registration.getWaybillNo())
                ? registration.getWaybillNo()
                : deliveryPlaceholderWaybill(registration);
        RentalDeliveryResult result = deliveryService.createOrReuseLocalOnly(
                new RentalDeliveryCreateCommand(
                        registration.getRentalOrderId(),
                        registration.getChannelOrderId(),
                        RentalDeliveryDirectionEnum.RETURN,
                        "CUSTOMER_RETURN_FORM",
                        registration.getFormNo(),
                        registration.getCarrierCode(),
                        registration.getCarrierName(),
                        waybillNo,
                        null,
                        commands));
        return result.deliveryId();
    }

    private String deliveryPlaceholderWaybill(RentalReturnRegistrationDO registration) {
        String method = registration.getReturnMethod() == null
                ? ReturnMethodEnum.EXPRESS.name()
                : registration.getReturnMethod();
        return method + "-" + registration.getFormNo();
    }

    private void addCandidate(Map<String, MatchedDevice> target, String serial,
                              RentalDeviceAssignmentDO assignment, RentalDeviceDO device) {
        String normalized = serialNormalizer.normalize(serial);
        if (!normalized.isBlank()) {
            target.putIfAbsent(normalized, new MatchedDevice(assignment, device));
        }
    }

    private Receipt receipt(RentalReturnRegistrationDO registration) {
        return new Receipt(registration.getFormNo(), registration.getStatus(),
                registration.getWaybillNo(), registration.getDeliveryId(), registration.getSubmittedAt());
    }

    private String normalizeWaybill(String value) {
        return value == null ? "" : value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record MatchedDevice(RentalDeviceAssignmentDO assignment, RentalDeviceDO device) {
    }

    private record MatchResult(boolean safe, List<MatchedDevice> devices) {
    }
}
