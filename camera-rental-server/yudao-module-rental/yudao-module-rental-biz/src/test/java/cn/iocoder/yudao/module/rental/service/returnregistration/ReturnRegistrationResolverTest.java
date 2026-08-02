package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_NOT_AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReturnRegistrationResolverTest {

    private final RentalReturnRegistrationMapper registrationMapper =
            mock(RentalReturnRegistrationMapper.class);
    private final ReturnRegistrationTokenService tokenService =
            new ReturnRegistrationTokenService();
    private final ReturnRegistrationResolver resolver =
            new ReturnRegistrationResolver(registrationMapper, tokenService);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void invalidTokenReturnsStableUnavailableErrorAndRestoresIgnoreState() {
        TenantContextHolder.setTenantId(77L);
        TenantContextHolder.setIgnore(false);

        ServiceException error =
                assertThrows(ServiceException.class, () -> resolver.require("invalid-token"));

        assertEquals(RETURN_REGISTRATION_NOT_AVAILABLE.getCode(), error.getCode());
        assertEquals(77L, TenantContextHolder.getRequiredTenantId());
        assertFalse(TenantContextHolder.isIgnore());
    }

    @Test
    void tenantExecutionRestoresPreviousTenantAndIgnoreState() {
        RentalReturnRegistrationDO registration = registration(9L);
        TenantContextHolder.setTenantId(77L);
        TenantContextHolder.setIgnore(true);

        Long result = resolver.execute(registration, () -> {
            assertEquals(9L, TenantContextHolder.getRequiredTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            return TenantContextHolder.getRequiredTenantId();
        });

        assertEquals(9L, result);
        assertEquals(77L, TenantContextHolder.getRequiredTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    @Test
    void resolvesExpiredAndRevokedLifecycleWithoutChangingTheRow() {
        RentalReturnRegistrationDO expired = registration(9L)
                .setStatus(ReturnRegistrationStatusEnum.DRAFT.name())
                .setExpiresAt(LocalDateTime.now().minusMinutes(1));
        RentalReturnRegistrationDO revoked = registration(9L)
                .setStatus(ReturnRegistrationStatusEnum.REVOKED.name())
                .setExpiresAt(LocalDateTime.now().plusDays(1));

        assertEquals(ReturnRegistrationStatusEnum.EXPIRED.name(),
                resolver.publicStatus(expired));
        assertEquals(ReturnRegistrationStatusEnum.REVOKED.name(),
                resolver.publicStatus(revoked));
    }

    @Test
    void sessionLookupRejectsExpiredSessionEvenForTerminalRegistration() {
        RentalReturnRegistrationDO accepted = registration(9L)
                .setStatus(ReturnRegistrationStatusEnum.ACCEPTED.name())
                .setExpiresAt(LocalDateTime.now().minusSeconds(1));
        ReturnRegistrationTokenService.IssuedToken token = tokenService.issue();
        when(registrationMapper.selectByTokenHash(token.hash())).thenReturn(accepted);

        assertThrows(ServiceException.class, () -> resolver.requireSession(token.plaintext()));
    }

    @Test
    void lockRejectsMissingOrCrossTenantRows() {
        RentalReturnRegistrationDO registration = registration(9L).setId(11L);
        when(registrationMapper.selectByIdForUpdate(11L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> resolver.lock(registration));

        RentalReturnRegistrationDO otherTenant = registration(10L).setId(11L);
        when(registrationMapper.selectByIdForUpdate(11L)).thenReturn(otherTenant);
        assertThrows(ServiceException.class, () -> resolver.lock(registration));
    }

    private RentalReturnRegistrationDO registration(Long tenantId) {
        RentalReturnRegistrationDO registration = new RentalReturnRegistrationDO()
                .setStatus(ReturnRegistrationStatusEnum.DRAFT.name())
                .setExpiresAt(LocalDateTime.now().plusDays(1));
        registration.setTenantId(tenantId);
        return registration;
    }
}
