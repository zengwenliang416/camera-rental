package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryTrackingRefreshServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalLogisticsProviderConfigService configService =
            mock(RentalLogisticsProviderConfigService.class);
    private final RentalDeliveryOutboxService outboxService = mock(RentalDeliveryOutboxService.class);
    private final RentalDeliveryTrackingRefreshService service = new RentalDeliveryTrackingRefreshService(
            deliveryMapper, configService, outboxService,
            Clock.fixed(Instant.parse("2026-07-31T04:00:00Z"), BUSINESS_ZONE));

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void queuesRefreshAfterLocalValidationAndReservesThrottle() {
        RentalDeliveryDO delivery = readyDelivery();
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        when(configService.get("KUAIDI100")).thenReturn(enabledConfig());
        when(configService.minimumQueryIntervalSeconds("KUAIDI100")).thenReturn(1800);
        when(outboxService.listPendingEventTypes(20L)).thenReturn(List.of());
        when(outboxService.enqueue(any(), any(), any(), any())).thenReturn(80L);

        RentalDeliveryRefreshResult result = service.refresh(20L);

        assertTrue(result.accepted());
        assertEquals("REFRESH_QUEUED", result.reason());
        assertEquals(LocalDateTime.of(2026, 7, 31, 12, 30), result.nextAllowedAt());
        assertEquals("QUEUED", delivery.getQueryStatus());
        assertNull(delivery.getNextQueryAllowedAt());
        verify(deliveryMapper).updateById(delivery);
        verify(outboxService).enqueue(20L, RentalDeliveryOutboxEventTypeEnum.REFRESH_QUERY,
                "manual:1785470400", "manual local tracking refresh");
    }

    @Test
    void returnsStableMappingAndThrottleReasonsWithoutEnqueue() {
        RentalDeliveryDO mappingRequired = readyDelivery();
        mappingRequired.setMappingStatus("MAPPING_REQUIRED");
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(mappingRequired);

        RentalDeliveryRefreshResult mappingResult = service.refresh(20L);

        assertFalse(mappingResult.accepted());
        assertEquals("MAPPING_REQUIRED", mappingResult.reason());
        verify(outboxService, never()).enqueue(any(), any(), any(), any());

        RentalDeliveryDO throttled = readyDelivery();
        throttled.setNextQueryAllowedAt(LocalDateTime.of(2026, 7, 31, 12, 10));
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 21L)).thenReturn(throttled);
        when(configService.get("KUAIDI100")).thenReturn(enabledConfig());

        RentalDeliveryRefreshResult throttleResult = service.refresh(21L);

        assertFalse(throttleResult.accepted());
        assertEquals("QUERY_THROTTLED", throttleResult.reason());
        assertEquals(LocalDateTime.of(2026, 7, 31, 12, 10), throttleResult.nextAllowedAt());
    }

    @Test
    void rejectsDisabledProviderAndAlreadyQueuedQuery() {
        RentalDeliveryDO delivery = readyDelivery();
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        when(configService.get("KUAIDI100")).thenReturn(RentalLogisticsProviderConfigDO.builder()
                .enabled(false).queryEnabled(false).build());

        assertEquals("PROVIDER_DISABLED", service.refresh(20L).reason());

        when(configService.get("KUAIDI100")).thenReturn(enabledConfig());
        when(outboxService.listPendingEventTypes(20L)).thenReturn(List.of("REFRESH_QUERY"));

        assertEquals("QUERY_ALREADY_QUEUED", service.refresh(20L).reason());
        verify(deliveryMapper, never()).updateById(delivery);
    }

    @Test
    void rejectsMissingDeliveryAndDisabledQueryWithStableReasons() {
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 99L)).thenReturn(null);
        assertEquals("DELIVERY_NOT_FOUND", service.refresh(99L).reason());

        RentalDeliveryDO delivery = readyDelivery();
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        when(configService.get("KUAIDI100")).thenReturn(RentalLogisticsProviderConfigDO.builder()
                .enabled(true).queryEnabled(false).build());

        assertEquals("QUERY_DISABLED", service.refresh(20L).reason());
        verify(outboxService, never()).enqueue(any(), any(), any(), any());
    }

    private RentalDeliveryDO readyDelivery() {
        return RentalDeliveryDO.builder()
                .id(20L)
                .mappingStatus("READY")
                .providerCode("KUAIDI100")
                .providerCarrierCode("shunfeng")
                .build();
    }

    private RentalLogisticsProviderConfigDO enabledConfig() {
        return RentalLogisticsProviderConfigDO.builder()
                .enabled(true)
                .queryEnabled(true)
                .minimumQueryIntervalSeconds(1800)
                .build();
    }
}
