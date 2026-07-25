package cn.iocoder.yudao.module.rental.controller.webhook.xianyu;

import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuOrderWebhookService;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuProductWebhookService;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuWebhookReceipt;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuOrderWebhookControllerTest {

    private final XianyuOrderWebhookService webhookService = mock(XianyuOrderWebhookService.class);
    private final XianyuProductWebhookService productWebhookService = mock(XianyuProductWebhookService.class);
    private final XianyuOrderWebhookController controller = new XianyuOrderWebhookController(
            webhookService, productWebhookService);

    @Test
    void shouldRejectDeclaredOversizedBodyBeforeReadingOrCallingService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(new byte[65_537]);

        XianyuWebhookReceipt receipt = controller.receiveOrder("app", 1L, "sign", request);

        assertEquals("fail", receipt.result());
        verify(webhookService, never()).receive(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldForwardExactUtf8BodyToService() {
        String body = "{\"user_name\":\"测试\"}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        when(webhookService.receive("app", 1L, "sign", body)).thenReturn(XianyuWebhookReceipt.success());

        XianyuWebhookReceipt receipt = controller.receiveOrder("app", 1L, "sign", request);

        assertEquals("success", receipt.result());
        verify(webhookService).receive("app", 1L, "sign", body);
    }

    @Test
    void shouldForwardProductBodyToProductService() {
        String body = "{\"product_id\":441160510721413}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        when(productWebhookService.receive("app", 1L, "sign", body))
                .thenReturn(XianyuWebhookReceipt.success());

        XianyuWebhookReceipt receipt = controller.receiveProduct("app", 1L, "sign", request);

        assertEquals("success", receipt.result());
        verify(productWebhookService).receive("app", 1L, "sign", body);
    }

}
