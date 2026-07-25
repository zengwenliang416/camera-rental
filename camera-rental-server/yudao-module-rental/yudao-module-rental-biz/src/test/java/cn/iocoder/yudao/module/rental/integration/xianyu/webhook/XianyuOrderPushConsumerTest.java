package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuOrderPushConsumerTest {

    @Mock
    private XianyuPushEventStateService stateService;
    @Mock
    private XianyuReadClient readClient;
    @Mock
    private XianyuOrderPersistenceService persistenceService;

    @Test
    void shouldMarkSafeFailureWhenDetailRefreshFails() {
        XianyuOrderPushConsumer consumer = new XianyuOrderPushConsumer(
                stateService, readClient, persistenceService, new ObjectMapper());
        when(stateService.claim(41L)).thenReturn("claim-1");
        when(readClient.execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any()))
                .thenThrow(new IllegalStateException("secret-bearing failure"));

        consumer.onOrderPush(new XianyuOrderPushReceivedEvent(9L, 41L, 77L, "order-1"));

        verify(stateService).markFailed(41L, "claim-1", "IllegalStateException");
        verify(stateService, never()).markSucceeded(any(), any());
        verify(persistenceService, never()).persistOrderDetail(any(), any());
    }

}
