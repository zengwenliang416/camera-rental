package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryCallbackInboxMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryOutboxService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RentalLogisticsOperationsTaskServiceTest {

    private final RentalLogisticsOperationsMapper operationsMapper = mock(RentalLogisticsOperationsMapper.class);
    private final RentalDeliveryOutboxMapper outboxMapper = mock(RentalDeliveryOutboxMapper.class);
    private final RentalDeliveryCallbackInboxMapper inboxMapper = mock(RentalDeliveryCallbackInboxMapper.class);
    private final RentalDeliveryOutboxService outboxService = mock(RentalDeliveryOutboxService.class);
    private final RentalLogisticsTaskOperationsService service = new RentalLogisticsTaskOperationsService(
            operationsMapper, outboxMapper, inboxMapper, outboxService);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void failedTaskQueryReturnsOnlySafeFieldsAndRedactedMessage() {
        RentalDeliveryCallbackInboxDO inbox = RentalDeliveryCallbackInboxDO.builder()
                .id(10L)
                .deliveryId(20L)
                .providerCode("KUAIDI100")
                .callbackParams("{\"phone\":\"13800138000\",\"address\":\"secret\"}")
                .processingStatus("FAILED")
                .retryCount(2)
                .lastErrorCode("provider.bad-signature")
                .lastErrorMessage("phone 13800138000 token ABCDEFGHIJKL")
                .receivedAt(LocalDateTime.of(2026, 7, 31, 10, 0))
                .build();
        when(operationsMapper.selectFailedInbox(9L, 20)).thenReturn(List.of(inbox));

        var tasks = service.listFailedTasks("INBOX", 20);

        assertEquals(1, tasks.size());
        assertEquals("PROVIDER_BAD_SIGNATURE", tasks.get(0).errorCode());
        assertEquals("PROVIDER_BAD_SIGNATURE", tasks.get(0).safeErrorMessage());
        assertFalse(tasks.get(0).safeErrorMessage().contains("13800138000"));
        assertFalse(tasks.get(0).safeErrorMessage().contains("ABCDEFGHIJKL"));
        assertFalse(tasks.get(0).toString().contains("address"));
    }

    @Test
    void outboxRetryClearsLeaseAndReturnsToPending() {
        RentalDeliveryOutboxDO task = RentalDeliveryOutboxDO.builder()
                .id(10L)
                .processingStatus("DEAD")
                .processingToken("lease-token")
                .leaseUntil(LocalDateTime.now())
                .nextAttemptAt(LocalDateTime.now())
                .lastErrorCode("FAILED")
                .lastErrorMessage("safe")
                .build();
        when(operationsMapper.selectOutboxForUpdate(9L, 10L)).thenReturn(task);

        var result = service.retry("OUTBOX", 10L);

        assertTrue(result.accepted());
        assertEquals("PENDING", task.getProcessingStatus());
        assertNull(task.getProcessingToken());
        assertNull(task.getLeaseUntil());
        assertNull(task.getNextAttemptAt());
        assertNull(task.getLastErrorMessage());
        verify(outboxMapper).updateById(task);
    }

    @Test
    void retryRejectsCrossTenantOrUnsafeState() {
        when(operationsMapper.selectInboxForUpdate(9L, 99L)).thenReturn(null);
        RentalLogisticsException missing = assertThrows(RentalLogisticsException.class,
                () -> service.retry("INBOX", 99L));
        assertEquals("FAILED_TASK_NOT_FOUND", missing.getCode());

        RentalDeliveryCallbackInboxDO succeeded = RentalDeliveryCallbackInboxDO.builder()
                .id(11L).processingStatus("SUCCEEDED").build();
        when(operationsMapper.selectInboxForUpdate(9L, 11L)).thenReturn(succeeded);
        RentalLogisticsException unsafe = assertThrows(RentalLogisticsException.class,
                () -> service.retry("INBOX", 11L));
        assertEquals("FAILED_TASK_STATE_NOT_RETRYABLE", unsafe.getCode());
        verifyNoInteractions(inboxMapper);
    }

    @Test
    void reconcileOnlyEnqueuesBoundedOutboxWork() {
        when(operationsMapper.selectReconcileCandidateIds(9L, 2)).thenReturn(List.of(100L, 101L));

        var result = service.reconcile(2);

        assertEquals(2, result.enqueuedCount());
        verify(outboxService).enqueue(eq(100L), eq(RentalDeliveryOutboxEventTypeEnum.RECONCILE),
                startsWith("manual:"), eq("manual reconcile"));
        verify(outboxService).enqueue(eq(101L), eq(RentalDeliveryOutboxEventTypeEnum.RECONCILE),
                startsWith("manual:"), eq("manual reconcile"));
        verifyNoMoreInteractions(outboxService);
    }
}
