package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryInboxLeaseServiceTest {

    private final RentalDeliveryCallbackInboxMapper inboxMapper =
            mock(RentalDeliveryCallbackInboxMapper.class);
    private final RentalDeliveryInboxLeaseService service = new RentalDeliveryInboxLeaseService(inboxMapper);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void reclaimsExpiredLeaseWithFreshToken() {
        RentalDeliveryCallbackInboxDO inbox = RentalDeliveryCallbackInboxDO.builder()
                .id(10L)
                .deliveryId(20L)
                .providerCode("KUAIDI100")
                .callbackParams("{}")
                .processingStatus("PROCESSING")
                .processingToken("expired-token")
                .leaseUntil(LocalDateTime.now().minusMinutes(1))
                .build();
        when(inboxMapper.selectClaimableForUpdate(eq(9L), any(), eq(20))).thenReturn(List.of(inbox));

        RentalInboxWorkItem work = service.claim(20).get(0);

        assertEquals("PROCESSING", inbox.getProcessingStatus());
        assertNotEquals("expired-token", work.processingToken());
        assertEquals(work.processingToken(), inbox.getProcessingToken());
        assertTrue(inbox.getLeaseUntil().isAfter(LocalDateTime.now()));
        verify(inboxMapper).updateById(inbox);
    }
}
