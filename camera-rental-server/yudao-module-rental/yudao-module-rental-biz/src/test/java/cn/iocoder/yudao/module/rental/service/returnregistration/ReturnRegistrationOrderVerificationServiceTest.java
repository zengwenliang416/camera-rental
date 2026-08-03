package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnRegistrationOrderVerificationServiceTest {

    private final XianyuOrderMapper xianyuOrderMapper = mock(XianyuOrderMapper.class);
    private final RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
    private final RentalReturnRegistrationMapper registrationMapper =
            mock(RentalReturnRegistrationMapper.class);
    private final ReturnRegistrationTokenService tokenService = new ReturnRegistrationTokenService();
    private final ReturnRegistrationPublicService publicService =
            mock(ReturnRegistrationPublicService.class);
    private final ReturnRegistrationSecurityAuditService auditService =
            mock(ReturnRegistrationSecurityAuditService.class);
    private final ReturnSerialNormalizer serialNormalizer = new ReturnSerialNormalizer();
    private final ReturnRegistrationOrderVerificationService service =
            new ReturnRegistrationOrderVerificationService(
                    xianyuOrderMapper, orderMapper, registrationMapper,
                    tokenService, publicService, auditService, serialNormalizer, 1L);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void verifiesUniqueChannelOrderAndCreatesTenantOwnedDraft() {
        XianyuOrderDO channelOrder = channelOrder(1L, "ORDER-001", "138-0013-8000");
        RentalOrderDO rentalOrder = rentalOrder(30L, 1L);
        ReturnRegistrationModels.PublicContext context = new ReturnRegistrationModels.PublicContext(
                "DRAFT", "RR1", "ORDER-001", "XIANYU",
                null, null, 1, LocalDateTime.now().plusHours(24), null);
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-001"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder);
        when(registrationMapper.selectLatestByChannelOrderIdForUpdate(11L)).thenReturn(null);
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(55L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        when(publicService.getSessionContext(anyString())).thenReturn(context);
        TenantContextHolder.setTenantId(77L);

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify(" ORDER-001 ", "8000", "P4-01");

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
    void verifiesUniqueMobileSuffixWithoutOrderNumber() {
        XianyuOrderDO channelOrder = channelOrder(1L, "ORDER-MOBILE-ONLY", "138-0013-8000");
        when(xianyuOrderMapper.selectListByReceiverMobileLast4("8000"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder(30L, 1L));
        when(registrationMapper.selectLatestByChannelOrderIdForUpdate(11L)).thenReturn(null);
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(56L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        ReturnRegistrationModels.PublicContext context = new ReturnRegistrationModels.PublicContext(
                "DRAFT", "RR2", "ORDER-MOBILE-ONLY", "XIANYU",
                null, null, 1, LocalDateTime.now().plusHours(24), null);
        when(publicService.getSessionContext(anyString())).thenReturn(context);

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify("", "8000", "P4-01");

        assertEquals(context, result.context());
        verify(xianyuOrderMapper, never()).selectListByExternalOrderId(anyString());
    }

    @Test
    void verifiesUniqueOrderNumberWithoutMobileSuffix() {
        XianyuOrderDO channelOrder = channelOrder(1L, "ORDER-ONLY", "138-0013-8000");
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-ONLY"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder(30L, 1L));
        when(registrationMapper.selectLatestByChannelOrderIdForUpdate(11L)).thenReturn(null);
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(57L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        ReturnRegistrationModels.PublicContext context = new ReturnRegistrationModels.PublicContext(
                "DRAFT", "RR3", "ORDER-ONLY", "XIANYU",
                null, null, 1, LocalDateTime.now().plusHours(24), null);
        when(publicService.getSessionContext(anyString())).thenReturn(context);

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify("ORDER-ONLY", "", "P4-01");

        assertEquals(context, result.context());
    }

    @Test
    void verifiesAssignedMachineCodeWhenOrderNumberAndMobileSuffixAreMissing() {
        XianyuOrderDO channelOrder = channelOrder(1L, "ORDER-MACHINE-ONLY", "13800138000");
        when(xianyuOrderMapper.selectListByAssignedMachineCode("P4-01"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder(30L, 1L));
        when(registrationMapper.selectLatestByChannelOrderIdForUpdate(11L)).thenReturn(null);
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(60L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        when(publicService.getSessionContext(anyString())).thenReturn(
                new ReturnRegistrationModels.PublicContext(
                        "DRAFT", "RR6", "ORDER-MACHINE-ONLY", "XIANYU",
                        null, null, 1, LocalDateTime.now().plusHours(24), null));

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify("", "", "p4 － 01");

        assertEquals(60L, result.registrationId());
        verify(xianyuOrderMapper, never()).selectListByExternalOrderId(anyString());
        verify(xianyuOrderMapper, never()).selectListByReceiverMobileLast4(anyString());
    }

    @Test
    void rejectsAmbiguousMobileSuffixWithoutSelectingAnOrder() {
        when(xianyuOrderMapper.selectListByReceiverMobileLast4("8000"))
                .thenReturn(List.of(
                        channelOrder(1L, "ORDER-001", "13800138000"),
                        channelOrder(1L, "ORDER-002", "13900138000")));

        assertVerificationFailure("", "8000", "P4-01");

        verify(xianyuOrderMapper, never()).selectByIdForUpdate(anyLong());
        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    @Test
    void rejectsMissingAmbiguousAndMobileMismatchWithSameError() {
        assertVerificationFailure("ORDER-MISSING", "8000", "P4-01");

        XianyuOrderDO first = channelOrder(1L, "ORDER-DUP", "13800138000");
        XianyuOrderDO second = channelOrder(1L, "ORDER-DUP", "13900139000");
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-DUP"))
                .thenReturn(List.of(first, second));
        assertVerificationFailure("ORDER-DUP", "8000", "P4-01");

        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-MOBILE"))
                .thenReturn(List.of(channelOrder(1L, "ORDER-MOBILE", "13800138000")));
        assertVerificationFailure("ORDER-MOBILE", "9999", "P4-01");

        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    @Test
    void revokedLatestRegistrationBlocksAutomaticReplacement() {
        XianyuOrderDO channelOrder = channelOrder(1L, "ORDER-REVOKED", "13800138000");
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-REVOKED"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder(30L, 1L));
        when(registrationMapper.selectLatestByChannelOrderIdForUpdate(11L))
                .thenReturn(registration(ReturnRegistrationStatusEnum.REVOKED));

        assertVerificationFailure("ORDER-REVOKED", "8000", "P4-01");
        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    @Test
    void comparesOnlyNormalizedReceiverMobileLastFour() {
        assertTrue(ReturnRegistrationOrderVerificationService.mobileMatches(
                "+86 138-0013-8000", "8000"));
    }

    @Test
    void orderNumberCanVerifyWhenRemoteNoLongerReturnsReceiverMobile() {
        XianyuOrderDO channelOrder = channelOrder(1L, "ORDER-NO-MOBILE", null);
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-NO-MOBILE"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(rentalOrder(30L, 1L));
        when(registrationMapper.selectLatestByChannelOrderIdForUpdate(11L)).thenReturn(null);
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(58L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        when(publicService.getSessionContext(anyString())).thenReturn(
                new ReturnRegistrationModels.PublicContext(
                        "DRAFT", "RR4", "ORDER-NO-MOBILE", "XIANYU",
                        null, null, 0, LocalDateTime.now().plusHours(24), null));

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify("ORDER-NO-MOBILE", "0000", "P4-01");

        assertEquals(58L, result.registrationId());
    }

    @Test
    void unconvertedOrderCreatesReviewableDraftWithoutAssignment() {
        XianyuOrderDO channelOrder = channelOrder(1L, "ORDER-PENDING-MAP", null);
        channelOrder.setRentalOrderId(null);
        when(xianyuOrderMapper.selectListByExternalOrderId("ORDER-PENDING-MAP"))
                .thenReturn(List.of(channelOrder));
        when(xianyuOrderMapper.selectByIdForUpdate(11L)).thenReturn(channelOrder);
        when(registrationMapper.selectLatestByChannelOrderIdForUpdate(11L)).thenReturn(null);
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(59L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        when(publicService.getSessionContext(anyString())).thenReturn(
                new ReturnRegistrationModels.PublicContext(
                        "DRAFT", "RR5", "ORDER-PENDING-MAP", null,
                        null, null, 0, LocalDateTime.now().plusHours(24), null));

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify("ORDER-PENDING-MAP", "", "P4-01");

        assertEquals(59L, result.registrationId());
        ArgumentCaptor<RentalReturnRegistrationDO> captor =
                ArgumentCaptor.forClass(RentalReturnRegistrationDO.class);
        verify(registrationMapper).insert(captor.capture());
        assertEquals(null, captor.getValue().getRentalOrderId());
        verify(orderMapper, never()).selectByIdForUpdate(anyLong());
    }

    @Test
    void unknownMachineCodeCreatesStandaloneJiezudaDraftForReview() {
        when(xianyuOrderMapper.selectListByAssignedMachineCode("P4-99"))
                .thenReturn(List.of());
        doAnswer(invocation -> {
            RentalReturnRegistrationDO value = invocation.getArgument(0);
            value.setId(61L);
            return 1;
        }).when(registrationMapper).insert(any(RentalReturnRegistrationDO.class));
        ReturnRegistrationModels.PublicContext context = new ReturnRegistrationModels.PublicContext(
                "DRAFT", "RR7", "", null,
                null, null, 0, LocalDateTime.now().plusHours(24), null);
        when(publicService.getSessionContext(anyString())).thenReturn(context);

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verify("", "", "P4-99");

        assertEquals(61L, result.registrationId());
        ArgumentCaptor<RentalReturnRegistrationDO> captor =
                ArgumentCaptor.forClass(RentalReturnRegistrationDO.class);
        verify(registrationMapper).insert(captor.capture());
        RentalReturnRegistrationDO saved = captor.getValue();
        assertEquals(1L, saved.getTenantId());
        assertEquals("", saved.getExternalOrderNo());
        assertEquals(null, saved.getRentalOrderId());
        assertEquals(null, saved.getChannelOrderId());
        verify(xianyuOrderMapper, never()).selectByIdForUpdate(anyLong());
    }

    @Test
    void rejectsOrderOwnedByAnotherTenant() {
        when(xianyuOrderMapper.selectListByExternalOrderId("OTHER-TENANT-ORDER"))
                .thenReturn(List.of(channelOrder(9L, "OTHER-TENANT-ORDER", "13800138000")));

        assertVerificationFailure("OTHER-TENANT-ORDER", "8000", "P4-01");

        verify(xianyuOrderMapper, never()).selectByIdForUpdate(anyLong());
        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    @Test
    void reusesOpenStandaloneSessionForPhotoThenSimpleSubmitFlow() {
        RentalReturnRegistrationDO standalone = new RentalReturnRegistrationDO()
                .setId(62L)
                .setRentalOrderId(null)
                .setChannelOrderId(null)
                .setExternalOrderNo("")
                .setStatus(ReturnRegistrationStatusEnum.DRAFT.name())
                .setExpiresAt(LocalDateTime.now().plusHours(1));
        standalone.setTenantId(1L);
        String sessionToken = tokenService.issue().plaintext();
        when(registrationMapper.selectByTokenHash(tokenService.hash(sessionToken)))
                .thenReturn(standalone);
        ReturnRegistrationModels.PublicContext context = new ReturnRegistrationModels.PublicContext(
                "DRAFT", "RR8", "", null,
                null, null, 0, standalone.getExpiresAt(), null);
        when(publicService.getSessionContext(sessionToken)).thenReturn(context);

        ReturnRegistrationOrderVerificationService.VerifiedSession result =
                service.verifyOrReuseStandalone(sessionToken, "", "", "P4-18");

        assertEquals(62L, result.registrationId());
        assertEquals(sessionToken, result.sessionToken());
        verify(xianyuOrderMapper, never()).selectListByAssignedMachineCode(anyString());
        verify(registrationMapper, never()).insert(any(RentalReturnRegistrationDO.class));
    }

    private void assertVerificationFailure(String orderNo, String mobileLast4, String machineCode) {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.verify(orderNo, mobileLast4, machineCode));
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
        value.setTenantId(1L);
        return value;
    }
}
