package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryOutboxCompletionServiceTest {

    private final RentalDeliveryOutboxMapper outboxMapper = mock(RentalDeliveryOutboxMapper.class);
    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalTrackingSnapshotService snapshotService = mock(RentalTrackingSnapshotService.class);
    private final RentalDeliveryOutboxCompletionService service = new RentalDeliveryOutboxCompletionService(
            outboxMapper, deliveryMapper, snapshotService, new RentalAsyncRetryPolicy(),
            new SensitiveValueRedactor());

    @Test
    void retryableFailureUsesBoundedRetryWaitAndSafeState() {
        RentalDeliveryOutboxDO task = RentalDeliveryOutboxDO.builder()
                .id(10L)
                .processingStatus("PROCESSING")
                .processingToken("lease-token")
                .retryCount(0)
                .build();
        RentalDeliveryDO delivery = RentalDeliveryDO.builder().id(20L).build();
        when(outboxMapper.selectById(10L)).thenReturn(task);
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        RentalOutboxWorkItem work = new RentalOutboxWorkItem(9L, 10L, "lease-token", 20L,
                30L, RentalDeliveryOutboxEventTypeEnum.INITIAL_QUERY, "KUAIDI100", null,
                null, null, null, null, null);

        service.complete(work, LogisticsOperationResult.failure("KUAIDI100_NETWORK_ERROR", true));

        assertEquals("RETRY_WAIT", task.getProcessingStatus());
        assertEquals(1, task.getRetryCount());
        assertNotNull(task.getNextAttemptAt());
        assertEquals("RETRY_WAIT", delivery.getQueryStatus());
        verify(outboxMapper).updateById(task);
        verify(deliveryMapper).updateById(delivery);
    }

    @Test
    void exhaustedFailureMovesTaskToDead() {
        RentalDeliveryOutboxDO task = RentalDeliveryOutboxDO.builder()
                .id(10L)
                .processingStatus("PROCESSING")
                .processingToken("lease-token")
                .retryCount(5)
                .build();
        RentalDeliveryDO delivery = RentalDeliveryDO.builder().id(20L).build();
        when(outboxMapper.selectById(10L)).thenReturn(task);
        when(deliveryMapper.selectByTenantIdAndIdForUpdate(9L, 20L)).thenReturn(delivery);
        RentalOutboxWorkItem work = new RentalOutboxWorkItem(9L, 10L, "lease-token", 20L,
                30L, RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE, "KUAIDI100", null,
                null, null, null, null, null);

        service.complete(work, LogisticsOperationResult.failure("KUAIDI100_NETWORK_ERROR", true));

        assertEquals("DEAD", task.getProcessingStatus());
        assertEquals("FAILED", delivery.getSubscribeStatus());
    }
}
