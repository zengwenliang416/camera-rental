package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnRegistrationPublicServiceTest {

    private final ReturnRegistrationResolver resolver = mock(ReturnRegistrationResolver.class);
    private final RentalReturnRegistrationMapper registrationMapper =
            mock(RentalReturnRegistrationMapper.class);
    private final RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper =
            mock(RentalDeviceAssignmentMapper.class);
    private final ReturnRegistrationPublicService service =
            new ReturnRegistrationPublicService(
                    resolver, registrationMapper, orderMapper, assignmentMapper);

    @Test
    void expiredLinkReturnsOnlySafeStatusWithoutOrderIdentity() throws Exception {
        RentalReturnRegistrationDO registration = registration(ReturnRegistrationStatusEnum.DRAFT)
                .setExternalOrderNo("ORDER-SECRET")
                .setExpiresAt(LocalDateTime.now().minusMinutes(1));
        stubExecution(registration);
        when(resolver.publicStatus(registration))
                .thenReturn(ReturnRegistrationStatusEnum.EXPIRED.name());

        ReturnRegistrationModels.PublicContext context = service.getContext("token");

        assertEquals(ReturnRegistrationStatusEnum.EXPIRED.name(), context.status());
        assertNull(context.formNo());
        assertNull(context.orderNo());
        assertNull(context.receipt());
        verify(orderMapper, never()).selectById(any());
        verify(assignmentMapper, never()).selectActiveListByRentalOrderId(any());
        verify(registrationMapper).updateById(registration);
    }

    @Test
    void revokedLinkReturnsOnlySafeStatusWithoutOrderIdentity() throws Exception {
        RentalReturnRegistrationDO registration = registration(ReturnRegistrationStatusEnum.REVOKED)
                .setExternalOrderNo("ORDER-SECRET");
        stubExecution(registration);
        when(resolver.publicStatus(registration))
                .thenReturn(ReturnRegistrationStatusEnum.REVOKED.name());

        ReturnRegistrationModels.PublicContext context = service.getContext("token");

        assertEquals(ReturnRegistrationStatusEnum.REVOKED.name(), context.status());
        assertNull(context.formNo());
        assertNull(context.orderNo());
        verify(orderMapper, never()).selectById(any());
    }

    private void stubExecution(RentalReturnRegistrationDO registration) throws Exception {
        when(resolver.require("token")).thenReturn(registration);
        when(registrationMapper.selectByIdForUpdate(registration.getId())).thenReturn(registration);
        when(resolver.execute(any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(1);
            return callable.call();
        });
    }

    private RentalReturnRegistrationDO registration(ReturnRegistrationStatusEnum status) {
        RentalReturnRegistrationDO registration = new RentalReturnRegistrationDO()
                .setId(11L)
                .setFormNo("RR202608010001")
                .setRentalOrderId(30L)
                .setStatus(status.name())
                .setExpiresAt(LocalDateTime.now().plusDays(1));
        registration.setTenantId(9L);
        return registration;
    }
}
