package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryInboxServiceImplTest {

    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalDeliveryCallbackInboxMapper inboxMapper = mock(RentalDeliveryCallbackInboxMapper.class);
    private final RentalDeliveryInboxService service =
            new RentalDeliveryInboxServiceImpl(deliveryMapper, inboxMapper);

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
    void persistsVerifiedCallbackForAsyncProcessing() {
        when(inboxMapper.selectByPayloadHashForUpdate(9L, "KUAIDI100", 10L, "payload-hash"))
                .thenReturn(RentalDeliveryCallbackInboxDO.builder().id(30L).deliveryId(10L).build());

        Long id = service.accept("KUAIDI100", 10L, "task-1", "payload-hash", "{\"data\":\"raw\"}");

        assertEquals(30L, id);
        ArgumentCaptor<RentalDeliveryCallbackInboxDO> captor =
                ArgumentCaptor.forClass(RentalDeliveryCallbackInboxDO.class);
        verify(inboxMapper).insertOrReuse(org.mockito.ArgumentMatchers.eq(9L), captor.capture());
        assertEquals("RECEIVED", captor.getValue().getProcessingStatus());
        assertEquals(0, captor.getValue().getRetryCount());
        assertEquals(10L, captor.getValue().getDeliveryId());
    }

    @Test
    void reusesDuplicatePayload() {
        when(inboxMapper.selectByPayloadHashForUpdate(9L, "KUAIDI100", 10L, "payload-hash"))
                .thenReturn(RentalDeliveryCallbackInboxDO.builder().id(31L).deliveryId(10L).build());

        Long id = service.accept("KUAIDI100", 10L, "task-1", "payload-hash", "{\"data\":\"raw\"}");

        assertEquals(31L, id);
        verify(inboxMapper).insertOrReuse(org.mockito.ArgumentMatchers.eq(9L),
                any(RentalDeliveryCallbackInboxDO.class));
        verify(deliveryMapper).updateById(any(RentalDeliveryDO.class));
    }

    @Test
    void scopesSamePayloadHashToDelivery() {
        when(deliveryMapper.selectByTenantIdAndId(9L, 11L))
                .thenReturn(RentalDeliveryDO.builder().id(11L).build());
        when(inboxMapper.selectByPayloadHashForUpdate(9L, "KUAIDI100", 11L, "payload-hash"))
                .thenReturn(RentalDeliveryCallbackInboxDO.builder().id(32L).deliveryId(11L).build());

        Long id = service.accept("KUAIDI100", 11L, "task-1", "payload-hash", "{\"data\":\"raw\"}");

        assertEquals(32L, id);
        verify(inboxMapper).selectByPayloadHashForUpdate(9L, "KUAIDI100", 11L, "payload-hash");
    }
}
