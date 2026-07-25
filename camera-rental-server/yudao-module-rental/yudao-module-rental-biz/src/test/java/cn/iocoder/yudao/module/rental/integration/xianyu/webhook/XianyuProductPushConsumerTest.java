package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuProductPushConsumerTest {

    @Mock
    private XianyuPushEventStateService stateService;
    @Mock
    private XianyuReadClient readClient;
    @Mock
    private XianyuProductPersistenceService productPersistenceService;

    @Test
    void shouldFetchReadOnlyProductDetailAndPersistIt() {
        when(stateService.claim(41L)).thenReturn("token");
        when(readClient.execute(eq(XianyuReadEndpoint.PRODUCT_DETAIL), any()))
                .thenReturn(new XianyuReadResponse(200, 0, new ObjectMapper().createObjectNode(), detailBody()));
        XianyuProductPushConsumer consumer = new XianyuProductPushConsumer(
                stateService, readClient, productPersistenceService, new ObjectMapper());

        consumer.onProductPush(new XianyuProductPushReceivedEvent(9L, 41L, 77L, "441160510721413"));

        verify(readClient).execute(eq(XianyuReadEndpoint.PRODUCT_DETAIL), any());
        verify(productPersistenceService).persistProductDetail(77L, detailBody());
        verify(stateService).markSucceeded(41L, "token");
    }

    private String detailBody() {
        return "{\"code\":0,\"data\":{\"product_id\":441160510721413}}";
    }

}
