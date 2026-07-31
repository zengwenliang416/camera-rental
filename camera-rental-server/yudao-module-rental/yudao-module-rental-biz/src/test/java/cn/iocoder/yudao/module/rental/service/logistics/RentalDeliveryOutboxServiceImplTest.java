package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryOutboxServiceImplTest {

    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalDeliveryOutboxMapper outboxMapper = mock(RentalDeliveryOutboxMapper.class);
    private final RentalDeliveryOutboxService service =
            new RentalDeliveryOutboxServiceImpl(deliveryMapper, outboxMapper, new SensitiveValueRedactor());

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
        when(deliveryMapper.selectByTenantIdAndId(9L, 10L))
                .thenReturn(RentalDeliveryDO.builder().id(10L).build());
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void storesOnlySafeIdentityAndRedactedMetadata() {
        when(outboxMapper.selectByDedupeKeyForUpdate(9L, "delivery:10:INITIAL_QUERY"))
                .thenReturn(RentalDeliveryOutboxDO.builder().id(20L).build());

        Long id = service.enqueue(10L, RentalDeliveryOutboxEventTypeEnum.INITIAL_QUERY, null,
                "phone=19900000000 waybill=SF1234567890");

        assertEquals(20L, id);
        ArgumentCaptor<RentalDeliveryOutboxDO> captor = ArgumentCaptor.forClass(RentalDeliveryOutboxDO.class);
        verify(outboxMapper).insertOrReuse(org.mockito.ArgumentMatchers.eq(9L), captor.capture());
        assertEquals("delivery:10:INITIAL_QUERY", captor.getValue().getDedupeKey());
        assertFalse(captor.getValue().getSafeMetadata().contains("19900000000"));
        assertFalse(captor.getValue().getSafeMetadata().contains("SF1234567890"));
    }

    @Test
    void reusesDuplicateTask() {
        when(outboxMapper.selectByDedupeKeyForUpdate(9L, "delivery:10:SUBSCRIBE"))
                .thenReturn(RentalDeliveryOutboxDO.builder().id(21L).build());

        Long id = service.enqueue(10L, RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE, null, null);

        assertEquals(21L, id);
        verify(outboxMapper).insertOrReuse(org.mockito.ArgumentMatchers.eq(9L),
                any(RentalDeliveryOutboxDO.class));
    }

    @Test
    void returnsOnlyPendingEventTypesInStableOrder() {
        when(outboxMapper.selectPendingByDeliveryId(9L, 10L)).thenReturn(List.of(
                RentalDeliveryOutboxDO.builder().id(20L).eventType("SUBSCRIBE").build(),
                RentalDeliveryOutboxDO.builder().id(21L).eventType("INITIAL_QUERY").build()));

        List<String> eventTypes = service.listPendingEventTypes(10L);

        assertEquals(List.of("SUBSCRIBE", "INITIAL_QUERY"), eventTypes);
    }
}
