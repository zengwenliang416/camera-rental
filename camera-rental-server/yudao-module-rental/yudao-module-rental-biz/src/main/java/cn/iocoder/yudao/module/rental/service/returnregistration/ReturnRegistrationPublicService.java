package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.PublicContext;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.Receipt;

@Service
public class ReturnRegistrationPublicService {

    private final ReturnRegistrationResolver resolver;
    private final RentalReturnRegistrationMapper registrationMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;

    public ReturnRegistrationPublicService(ReturnRegistrationResolver resolver,
                                           RentalReturnRegistrationMapper registrationMapper,
                                           RentalOrderMapper orderMapper,
                                           RentalDeviceAssignmentMapper assignmentMapper) {
        this.resolver = resolver;
        this.registrationMapper = registrationMapper;
        this.orderMapper = orderMapper;
        this.assignmentMapper = assignmentMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public PublicContext getContext(String token) {
        RentalReturnRegistrationDO registration = resolver.require(token);
        return resolver.execute(registration, () -> {
            RentalReturnRegistrationDO current = registrationMapper.selectByIdForUpdate(registration.getId());
            String status = resolver.publicStatus(current);
            if (ReturnRegistrationStatusEnum.EXPIRED.name().equals(status)
                    && !status.equals(current.getStatus())) {
                current.setStatus(status);
                registrationMapper.updateById(current);
            } else if (current.getOpenedAt() == null
                    && ReturnRegistrationStatusEnum.DRAFT.name().equals(status)) {
                current.setOpenedAt(LocalDateTime.now());
                registrationMapper.updateById(current);
            }
            if (ReturnRegistrationStatusEnum.EXPIRED.name().equals(status)
                    || ReturnRegistrationStatusEnum.REVOKED.name().equals(status)) {
                return new PublicContext(status, null, null, null,
                        null, null, 0, current.getExpiresAt(), null);
            }
            RentalOrderDO order = orderMapper.selectById(current.getRentalOrderId());
            Receipt receipt = ReturnRegistrationStatusEnum.DRAFT.name().equals(status)
                    ? null
                    : new Receipt(current.getFormNo(), status, current.getWaybillNo(),
                    current.getDeliveryId(), current.getSubmittedAt());
            return new PublicContext(status, current.getFormNo(), current.getExternalOrderNo(),
                    order == null ? null : order.getSourceType(),
                    order == null ? null : order.getBillableStartDate(),
                    order == null ? null : order.getBillableEndDate(),
                    assignmentMapper.selectActiveListByRentalOrderId(current.getRentalOrderId()).size(),
                    current.getExpiresAt(), receipt);
        });
    }
}
