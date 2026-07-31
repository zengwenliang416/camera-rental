package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryInboxCompletionServiceTest {

    private final RentalDeliveryCallbackInboxMapper inboxMapper =
            mock(RentalDeliveryCallbackInboxMapper.class);
    private final RentalTrackingSnapshotService snapshotService = mock(RentalTrackingSnapshotService.class);
    private final RentalDeliveryInboxCompletionService service = new RentalDeliveryInboxCompletionService(
            inboxMapper, snapshotService, new RentalAsyncRetryPolicy());

    @Test
    void retryableFailureSchedulesBoundedRetry() {
        RentalDeliveryCallbackInboxDO inbox = processingInbox(0);
        when(inboxMapper.selectById(10L)).thenReturn(inbox);

        service.complete(work(), LogisticsOperationResult.failure("CALLBACK_PROCESSING_ERROR", true));

        assertEquals("RETRY_WAIT", inbox.getProcessingStatus());
        assertEquals(1, inbox.getRetryCount());
        assertNotNull(inbox.getNextRetryAt());
        assertNull(inbox.getProcessingToken());
        assertNull(inbox.getLeaseUntil());
        verify(inboxMapper).updateById(inbox);
    }

    @Test
    void exhaustedFailureMovesInboxToDead() {
        RentalDeliveryCallbackInboxDO inbox = processingInbox(5);
        when(inboxMapper.selectById(10L)).thenReturn(inbox);

        service.complete(work(), LogisticsOperationResult.failure("CALLBACK_PROCESSING_ERROR", true));

        assertEquals("DEAD", inbox.getProcessingStatus());
        assertEquals(6, inbox.getRetryCount());
        assertNull(inbox.getNextRetryAt());
    }

    private RentalDeliveryCallbackInboxDO processingInbox(int retryCount) {
        return RentalDeliveryCallbackInboxDO.builder()
                .id(10L)
                .deliveryId(20L)
                .processingStatus("PROCESSING")
                .processingToken("lease-token")
                .retryCount(retryCount)
                .build();
    }

    private RentalInboxWorkItem work() {
        return new RentalInboxWorkItem(9L, 10L, "lease-token", 20L, "KUAIDI100", "{}");
    }
}
