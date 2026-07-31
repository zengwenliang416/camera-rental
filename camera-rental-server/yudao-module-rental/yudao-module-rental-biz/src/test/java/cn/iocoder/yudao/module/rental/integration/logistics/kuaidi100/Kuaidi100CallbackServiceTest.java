package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.service.logistics.LogisticsHashing;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryInboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Kuaidi100CallbackServiceTest {

    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalDeliveryInboxService inboxService = mock(RentalDeliveryInboxService.class);
    private final LogisticsHashing hashing = new LogisticsHashing();
    private final Kuaidi100Signer signer = new Kuaidi100Signer();
    private final Kuaidi100CallbackService service =
            new Kuaidi100CallbackService(deliveryMapper, inboxService, hashing, signer);

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void locatesTenantVerifiesSignatureAndPersistsInboxBeforeAck() {
        String token = "fixture-callback-token";
        String param = "{\"status\":\"polling\",\"lastResult\":{\"status\":\"200\",\"data\":[]}}";
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(88L)
                .callbackToken(token)
                .callbackSalt("fixture-delivery-salt")
                .build();
        delivery.setTenantId(9L);
        when(deliveryMapper.selectCallbackCandidatesByTokenHash(hashing.sha256(token)))
                .thenReturn(List.of(delivery));
        doAnswer(invocation -> {
            assertEquals(9L, TenantContextHolder.getRequiredTenantId());
            return 99L;
        }).when(inboxService).accept(eq("KUAIDI100"), eq(88L), eq(null),
                eq(hashing.sha256(param)), eq(param));

        Kuaidi100CallbackReceipt receipt = service.receive(
                token, param, signer.signCallback(param, "fixture-delivery-salt"));

        assertTrue(receipt.result());
        assertEquals("200", receipt.returnCode());
        verify(inboxService).accept("KUAIDI100", 88L, null, hashing.sha256(param), param);
    }

    @Test
    void rejectsInvalidSignatureWithoutInboxWrite() {
        String token = "fixture-callback-token";
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(88L)
                .callbackToken(token)
                .callbackSalt("fixture-delivery-salt")
                .build();
        delivery.setTenantId(9L);
        when(deliveryMapper.selectCallbackCandidatesByTokenHash(any())).thenReturn(List.of(delivery));

        Kuaidi100CallbackReceipt receipt = service.receive(token, "{}", "INVALID");

        assertFalse(receipt.result());
        verify(inboxService, never()).accept(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsOversizedPayloadBeforeDeliveryLookup() {
        String oversized = "x".repeat(262_145);

        Kuaidi100CallbackReceipt receipt = service.receive("callback-token", oversized, "SIGN");

        assertFalse(receipt.result());
        verify(deliveryMapper, never()).selectCallbackCandidatesByTokenHash(any());
        verify(inboxService, never()).accept(any(), any(), any(), any(), any());
    }

    @Test
    void selectsExactTokenFromCrossTenantHashCandidates() {
        String token = "fixture-callback-token";
        String param = "{}";
        RentalDeliveryDO otherTenantCollision = RentalDeliveryDO.builder()
                .id(77L)
                .callbackToken("different-token-with-same-stored-hash")
                .callbackSalt("other-salt")
                .build();
        otherTenantCollision.setTenantId(8L);
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(88L)
                .callbackToken(token)
                .callbackSalt("fixture-delivery-salt")
                .build();
        delivery.setTenantId(9L);
        when(deliveryMapper.selectCallbackCandidatesByTokenHash(hashing.sha256(token)))
                .thenReturn(List.of(otherTenantCollision, delivery));

        Kuaidi100CallbackReceipt receipt = service.receive(
                token, param, signer.signCallback(param, "fixture-delivery-salt"));

        assertTrue(receipt.result());
        verify(inboxService).accept("KUAIDI100", 88L, null, hashing.sha256(param), param);
    }

    @Test
    void rejectsAmbiguousTokenReusedAcrossTenants() {
        String token = "fixture-callback-token";
        RentalDeliveryDO first = RentalDeliveryDO.builder()
                .id(77L)
                .callbackToken(token)
                .callbackSalt("first-salt")
                .build();
        first.setTenantId(8L);
        RentalDeliveryDO second = RentalDeliveryDO.builder()
                .id(88L)
                .callbackToken(token)
                .callbackSalt("second-salt")
                .build();
        second.setTenantId(9L);
        when(deliveryMapper.selectCallbackCandidatesByTokenHash(hashing.sha256(token)))
                .thenReturn(List.of(first, second));

        Kuaidi100CallbackReceipt receipt = service.receive(
                token, "{}", signer.signCallback("{}", "first-salt"));

        assertFalse(receipt.result());
        verify(inboxService, never()).accept(any(), any(), any(), any(), any());
    }
}
