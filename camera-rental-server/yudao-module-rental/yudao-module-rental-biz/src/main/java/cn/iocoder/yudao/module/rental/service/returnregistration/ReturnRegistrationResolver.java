package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_NOT_AVAILABLE;

@Service
public class ReturnRegistrationResolver {

    private final RentalReturnRegistrationMapper registrationMapper;
    private final ReturnRegistrationTokenService tokenService;

    public ReturnRegistrationResolver(RentalReturnRegistrationMapper registrationMapper,
                                      ReturnRegistrationTokenService tokenService) {
        this.registrationMapper = registrationMapper;
        this.tokenService = tokenService;
    }

    public RentalReturnRegistrationDO require(String token) {
        RentalReturnRegistrationDO registration = TenantUtils.executeIgnore(
                () -> registrationMapper.selectByTokenHash(tokenService.hash(token)));
        if (registration == null) {
            throw exception(RETURN_REGISTRATION_NOT_AVAILABLE);
        }
        return registration;
    }

    public <T> T execute(RentalReturnRegistrationDO registration, Callable<T> callable) {
        return TenantUtils.execute(registration.getTenantId(), callable);
    }

    public RentalReturnRegistrationDO lock(RentalReturnRegistrationDO registration) {
        RentalReturnRegistrationDO current =
                registrationMapper.selectByIdForUpdate(registration.getId());
        if (current == null || !registration.getTenantId().equals(current.getTenantId())) {
            throw exception(RETURN_REGISTRATION_NOT_AVAILABLE);
        }
        return current;
    }

    public String publicStatus(RentalReturnRegistrationDO registration) {
        if (ReturnRegistrationStatusEnum.DRAFT.name().equals(registration.getStatus())
                && registration.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ReturnRegistrationStatusEnum.EXPIRED.name();
        }
        return registration.getStatus();
    }
}
