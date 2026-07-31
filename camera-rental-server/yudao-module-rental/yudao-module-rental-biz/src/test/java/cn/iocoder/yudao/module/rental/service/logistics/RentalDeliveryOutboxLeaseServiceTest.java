package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RentalDeliveryOutboxLeaseServiceTest {

    private final RentalDeliveryOutboxMapper outboxMapper = mock(RentalDeliveryOutboxMapper.class);
    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalLogisticsProviderConfigService configService =
            mock(RentalLogisticsProviderConfigService.class);
    private final RentalLogisticsProviderCredentialService credentialService =
            mock(RentalLogisticsProviderCredentialService.class);
    private final RentalDeliveryOutboxLeaseService service = new RentalDeliveryOutboxLeaseService(
            outboxMapper, deliveryMapper, configService, credentialService, new LogisticsHashing());

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void throttlesQueryWithoutExposingDeliverySecretsInWorkItem() {
        RentalDeliveryOutboxDO task = RentalDeliveryOutboxDO.builder()
                .id(10L)
                .deliveryId(20L)
                .eventType("INITIAL_QUERY")
                .processingStatus("PENDING")
                .build();
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(20L)
                .providerCode("KUAIDI100")
                .providerCarrierCode("shunfeng")
                .waybillNo("SF0000000001")
                .nextQueryAllowedAt(LocalDateTime.now().plusMinutes(10))
                .build();
        when(outboxMapper.selectClaimableForUpdate(eq(9L), any(), eq(20))).thenReturn(List.of(task));
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        when(configService.get("KUAIDI100")).thenReturn(RentalLogisticsProviderConfigDO.builder()
                .enabled(true)
                .queryEnabled(true)
                .build());
        stubCredential(delivery);

        RentalOutboxWorkItem work = service.claim(20).get(0);

        assertEquals("QUERY_THROTTLED", work.skipCode());
        assertEquals(null, work.waybillNo());
    }

    @Test
    void createsPerDeliveryCallbackIdentityForSubscription() {
        RentalDeliveryOutboxDO task = RentalDeliveryOutboxDO.builder()
                .id(10L)
                .deliveryId(20L)
                .eventType("SUBSCRIBE")
                .processingStatus("PENDING")
                .build();
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(20L)
                .providerCode("KUAIDI100")
                .providerCarrierCode("shunfeng")
                .waybillNo("SF0000000001")
                .subscribeCount(0)
                .build();
        when(outboxMapper.selectClaimableForUpdate(eq(9L), any(), eq(20))).thenReturn(List.of(task));
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        when(configService.get("KUAIDI100")).thenReturn(RentalLogisticsProviderConfigDO.builder()
                .enabled(true)
                .subscribeEnabled(true)
                .callbackBaseUrl("https://example.test/")
                .build());
        stubCredential(delivery);

        RentalOutboxWorkItem work = service.claim(20).get(0);

        assertEquals(30L, work.credentialId());
        assertEquals(30L, delivery.getProviderCredentialId());
        assertNotNull(work.callbackSalt());
        assertNotNull(delivery.getCallbackTokenHash());
        assertTrue(work.callbackUrl().startsWith("https://example.test/rental/webhooks/kuaidi100/tracking/"));
    }

    @Test
    void blocksSubscriptionAfterMonthlyAttemptLimitWithoutExposingSecrets() {
        RentalDeliveryOutboxDO task = RentalDeliveryOutboxDO.builder()
                .id(10L)
                .deliveryId(20L)
                .eventType("SUBSCRIBE")
                .processingStatus("PENDING")
                .build();
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .id(20L)
                .providerCode("KUAIDI100")
                .providerCarrierCode("shunfeng")
                .waybillNo("SF0000000001")
                .subscribeMonth(YearMonth.now(ZoneId.of("Asia/Shanghai")).toString())
                .subscribeCount(10)
                .build();
        when(outboxMapper.selectClaimableForUpdate(eq(9L), any(), eq(20))).thenReturn(List.of(task));
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        when(configService.get("KUAIDI100")).thenReturn(RentalLogisticsProviderConfigDO.builder()
                .enabled(true)
                .subscribeEnabled(true)
                .callbackBaseUrl("https://example.test/")
                .build());
        stubCredential(delivery);

        RentalOutboxWorkItem work = service.claim(20).get(0);

        assertEquals("SUBSCRIBE_MONTHLY_LIMIT", work.skipCode());
        assertEquals(null, work.waybillNo());
        assertEquals(null, work.callbackUrl());
    }

    private void stubCredential(RentalDeliveryDO delivery) {
        when(credentialService.resolveForDelivery(delivery))
                .thenReturn(RentalLogisticsProviderCredentialDO.builder()
                        .id(30L)
                        .providerCode("KUAIDI100")
                        .enabled(true)
                        .customerCode("customer")
                        .apiKey("key")
                        .build());
    }
}
