package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_VERIFICATION_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnRegistrationOrderVerificationServiceTest {

    private final XianyuOrderMapper xianyuOrderMapper = mock(XianyuOrderMapper.class);
    private final RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper =
            mock(RentalDeviceAssignmentMapper.class);
    private final RentalReturnRegistrationMapper registrationMapper =
            mock(RentalReturnRegistrationMapper.class);
    private final ReturnRegistrationTokenService tokenService = new ReturnRegistrationTokenService();
    private final ReturnRegistrationPublicService publicService =
            mock(ReturnRegistrationPublicService.class);
    private final ReturnRegistrationSecurityAuditService auditService =
            mock(ReturnRegistrationSecurityAuditService.class);
    private final ReturnRegistrationOrderVerificationService service =
            new ReturnRegistrationOrderVerificationService(
                    xianyuOrderMapper, orderMapper, assignmentMapper, registrationMapper,
                    tokenService, publicService, auditService);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void verifiesUniqueChannelOrderAndCreatesTenantOwnedDraft() {
        XianyuOrderDO channelOrder = channelOrder(9L, "ORDER-001", "138-0013-8000");
        RentalOrderDO rentalOrder = rentalOrder(30L, 9L);
        ReturnRegistrationModels.PublicContext context = new ReturnRegistrationModels.PublicContext(
                "DRAFT", "RR1", "ORDER-001", "XIANYU",
                null, null, 1, LocalDateTime.now().plusHours(24), null);
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-001"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder);
        when(assignmentMapper.selectActiveListByRentalOrderId(30L))
                .thenReturn(List.of(new RentalDeviceAssignmentDO()));
        when(registrationMapper.selectLatestByRentalOrderIdForUpdate(30L)).thenReturn(null);
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(55L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        when(publicService.getSessionContext(anyString())).thenReturn(context);
        TenantContextHolder.setTenantId(77L);

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify(" ORDER-001 ", "8000");

        assertEquals(context, result.context());
        assertEquals(55L, result.registrationId());
        assertEquals(77L, TenantContextHolder.getRequiredTenantId());
        ArgumentCaptor<RentalReturnRegistrationDO> captor =
                ArgumentCaptor.forClass(RentalReturnRegistrationDO.class);
        verify(registrationMapper).insert(captor.capture());
        RentalReturnRegistrationDO saved = captor.getValue();
        assertEquals("ORDER-001", saved.getExternalOrderNo());
        assertEquals(30L, saved.getRentalOrderId());
        assertEquals(11L, saved.getChannelOrderId());
        assertEquals(ReturnRegistrationStatusEnum.DRAFT.name(), saved.getStatus());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
        assertNotEquals(tokenService.hash(result.sessionToken()), result.sessionToken());
        assertEquals(tokenService.hash(result.sessionToken()), saved.getTokenHash());
    }

    @Test
    void rejectsMissingAmbiguousAndMobileMismatchWithSameError() {
        assertVerificationFailure("ORDER-MISSING", "8000");

        XianyuOrderDO first = channelOrder(9L, "ORDER-DUP", "13800138000");
        XianyuOrderDO second = channelOrder(10L, "ORDER-DUP", "13900139000");
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-DUP"))
                .thenReturn(List.of(first, second));
        assertVerificationFailure("ORDER-DUP", "8000");

        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-MOBILE"))
                .thenReturn(List.of(channelOrder(9L, "ORDER-MOBILE", "13800138000")));
        assertVerificationFailure("ORDER-MOBILE", "9999");

        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    @Test
    void revokedLatestRegistrationBlocksAutomaticReplacement() {
        XianyuOrderDO channelOrder = channelOrder(9L, "ORDER-REVOKED", "13800138000");
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-REVOKED"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder(30L, 9L));
        when(assignmentMapper.selectActiveListByRentalOrderId(30L))
                .thenReturn(List.of(new RentalDeviceAssignmentDO()));
        when(registrationMapper.selectLatestByRentalOrderIdForUpdate(30L))
                .thenReturn(registration(ReturnRegistrationStatusEnum.REVOKED));

        assertVerificationFailure("ORDER-REVOKED", "8000");
        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    @Test
    void comparesOnlyNormalizedReceiverMobileLastFour() {
        assertTrue(ReturnRegistrationOrderVerificationService.mobileMatches(
                "+86 138-0013-8000", "8000"));
    }

    @Test
    void missingReceiverMobileCannotBeVerifiedWithZeroes() {
        XianyuOrderDO channelOrder = channelOrder(9L, "ORDER-NO-MOBILE", null);
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-NO-MOBILE"))
                .thenReturn(List.of(channelOrder));

        assertVerificationFailure("ORDER-NO-MOBILE", "0000");
        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    private void assertVerificationFailure(String orderNo, String mobileLast4) {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.verify(orderNo, mobileLast4));
        assertEquals(RETURN_REGISTRATION_VERIFICATION_FAILED.getCode(), error.getCode());
        assertEquals(RETURN_REGISTRATION_VERIFICATION_FAILED.getMsg(), error.getMessage());
    }

    private XianyuOrderDO channelOrder(Long tenantId, String orderNo, String mobile) {
        XianyuOrderDO value = XianyuOrderDO.builder()
                .id(11L)
                .externalOrderId(orderNo)
                .receiverMobile(mobile)
                .rentalOrderId(30L)
                .build();
        value.setTenantId(tenantId);
        return value;
    }

    private RentalOrderDO rentalOrder(Long id, Long tenantId) {
        RentalOrderDO value = RentalOrderDO.builder().id(id).build();
        value.setTenantId(tenantId);
        return value;
    }

    private RentalReturnRegistrationDO registration(ReturnRegistrationStatusEnum status) {
        RentalReturnRegistrationDO value = new RentalReturnRegistrationDO()
                .setId(50L)
                .setStatus(status.name())
                .setExpiresAt(LocalDateTime.now().plusHours(1));
        value.setTenantId(9L);
        return value;
    }
}
