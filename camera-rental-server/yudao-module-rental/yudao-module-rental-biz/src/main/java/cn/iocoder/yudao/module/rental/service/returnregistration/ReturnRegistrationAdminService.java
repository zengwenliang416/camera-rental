package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_NOT_AVAILABLE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_ORDER_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_STATUS_INVALID;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminCreateResult;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminCustomerView;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminDetail;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminDeviceView;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AdminRow;

@Service
public class ReturnRegistrationAdminService {

    private final RentalReturnRegistrationMapper registrationMapper;
    private final RentalReturnRegistrationDeviceMapper registrationDeviceMapper;
    private final RentalOrderMapper orderMapper;
    private final XianyuOrderMapper xianyuOrderMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final ReturnRegistrationTokenService tokenService;
    private final ReturnRegistrationAttachmentService attachmentService;
    private final ReturnRegistrationSubmissionService submissionService;

    public ReturnRegistrationAdminService(
            RentalReturnRegistrationMapper registrationMapper,
            RentalReturnRegistrationDeviceMapper registrationDeviceMapper,
            RentalOrderMapper orderMapper,
            XianyuOrderMapper xianyuOrderMapper,
            RentalDeviceAssignmentMapper assignmentMapper,
            ReturnRegistrationTokenService tokenService,
            ReturnRegistrationAttachmentService attachmentService,
            ReturnRegistrationSubmissionService submissionService) {
        this.registrationMapper = registrationMapper;
        this.registrationDeviceMapper = registrationDeviceMapper;
        this.orderMapper = orderMapper;
        this.xianyuOrderMapper = xianyuOrderMapper;
        this.assignmentMapper = assignmentMapper;
        this.tokenService = tokenService;
        this.attachmentService = attachmentService;
        this.submissionService = submissionService;
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCreateResult create(Long rentalOrderId, Integer validDays) {
        RentalOrderDO order = orderMapper.selectByIdForUpdate(rentalOrderId);
        if (order == null || assignmentMapper.selectActiveListByRentalOrderId(rentalOrderId).isEmpty()) {
            throw exception(RETURN_REGISTRATION_ORDER_INVALID);
        }
        int days = validDays == null ? 7 : Math.max(1, Math.min(validDays, 30));
        ReturnRegistrationTokenService.IssuedToken token = tokenService.issue();
        XianyuOrderDO channelOrder = order.getChannelOrderId() == null ? null
                : xianyuOrderMapper.selectById(order.getChannelOrderId());
        String formNo = "RR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
        RentalReturnRegistrationDO registration = new RentalReturnRegistrationDO()
                .setFormNo(formNo)
                .setRentalOrderId(order.getId())
                .setChannelOrderId(order.getChannelOrderId())
                .setExternalOrderNo(channelOrder == null
                        ? order.getSourceOrderId() : channelOrder.getExternalOrderId())
                .setTokenHash(token.hash())
                .setStatus(ReturnRegistrationStatusEnum.DRAFT.name())
                .setExpiresAt(LocalDateTime.now().plusDays(days));
        registrationMapper.insert(registration);
        return new AdminCreateResult(registration.getId(), formNo, token.plaintext(),
                "/return/" + token.plaintext(), registration.getExpiresAt());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCreateResult reissue(Long id, Integer validDays) {
        RentalReturnRegistrationDO registration = registrationMapper.selectByIdForUpdate(id);
        if (registration == null
                || !ReturnRegistrationStatusEnum.DRAFT.name().equals(registration.getStatus())) {
            throw exception(RETURN_REGISTRATION_STATUS_INVALID);
        }
        int days = validDays == null ? 7 : Math.max(1, Math.min(validDays, 30));
        ReturnRegistrationTokenService.IssuedToken token = tokenService.issue();
        registration.setTokenHash(token.hash())
                .setExpiresAt(LocalDateTime.now().plusDays(days))
                .setOpenedAt(null);
        registrationMapper.updateById(registration);
        return new AdminCreateResult(registration.getId(), registration.getFormNo(),
                token.plaintext(), "/return/" + token.plaintext(), registration.getExpiresAt());
    }

    public PageResult<AdminRow> page(PageParam page, String status, Long orderId, String keyword,
                                     String serial, LocalDateTime submittedStart,
                                     LocalDateTime submittedEnd) {
        PageResult<RentalReturnRegistrationDO> values =
                registrationMapper.selectPage(page, status, orderId, keyword,
                        serial, submittedStart, submittedEnd);
        List<AdminRow> rows = values.getList().stream().map(value -> new AdminRow(
                value.getId(), value.getFormNo(), value.getRentalOrderId(),
                value.getExternalOrderNo(), value.getStatus(), value.getCarrierName(),
                value.getWaybillNo(), value.getExpiresAt(), value.getSubmittedAt(),
                value.getCreateTime())).toList();
        return new PageResult<>(rows, values.getTotal());
    }

    public AdminDetail get(Long id) {
        RentalReturnRegistrationDO registration = require(id);
        List<AdminDeviceView> devices = registrationDeviceMapper.selectListByRegistrationId(id)
                .stream().map(this::deviceView).toList();
        XianyuOrderDO channelOrder = registration.getChannelOrderId() == null ? null
                : xianyuOrderMapper.selectById(registration.getChannelOrderId());
        AdminCustomerView customer = channelOrder == null ? null : new AdminCustomerView(
                channelOrder.getReceiverName(), channelOrder.getReceiverMobile(),
                channelOrder.getReceiverAddress());
        return new AdminDetail(registration.getId(), registration.getFormNo(),
                registration.getRentalOrderId(), registration.getExternalOrderNo(),
                registration.getStatus(), registration.getCarrierCode(), registration.getCarrierName(),
                registration.getWaybillNo(), registration.getShippedDate(),
                registration.getIssueDescription(), registration.getDeliveryId(),
                registration.getExpiresAt(), registration.getSubmittedAt(),
                registration.getReviewedAt(), registration.getReviewerId(),
                registration.getReviewNote(), customer, devices, attachmentService.listForAdmin(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        RentalReturnRegistrationDO registration = registrationMapper.selectByIdForUpdate(id);
        if (registration == null || !ReturnRegistrationStatusEnum.DRAFT.name().equals(registration.getStatus())) {
            throw exception(RETURN_REGISTRATION_STATUS_INVALID);
        }
        registration.setStatus(ReturnRegistrationStatusEnum.REVOKED.name());
        registrationMapper.updateById(registration);
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, boolean accept, String note, Long reviewerId) {
        if (accept) {
            submissionService.acceptReview(id);
            RentalReturnRegistrationDO accepted = registrationMapper.selectByIdForUpdate(id);
            accepted.setReviewerId(reviewerId).setReviewNote(note);
            registrationMapper.updateById(accepted);
            return;
        }
        RentalReturnRegistrationDO registration = registrationMapper.selectByIdForUpdate(id);
        if (registration == null
                || !ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name().equals(registration.getStatus())) {
            throw exception(RETURN_REGISTRATION_STATUS_INVALID);
        }
        registration.setStatus(ReturnRegistrationStatusEnum.REJECTED.name())
                .setReviewerId(reviewerId)
                .setReviewNote(note)
                .setReviewedAt(LocalDateTime.now());
        registrationMapper.updateById(registration);
    }

    private RentalReturnRegistrationDO require(Long id) {
        RentalReturnRegistrationDO registration = registrationMapper.selectById(id);
        if (registration == null
                || !TenantContextHolder.getRequiredTenantId().equals(registration.getTenantId())) {
            throw exception(RETURN_REGISTRATION_NOT_AVAILABLE);
        }
        return registration;
    }

    private AdminDeviceView deviceView(RentalReturnRegistrationDeviceDO value) {
        return new AdminDeviceView(value.getSubmittedSerial(), value.getNormalizedSerial(),
                value.getMatchStatus(), value.getMatchMessage(), value.getDeviceId());
    }
}
