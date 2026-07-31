package cn.iocoder.yudao.module.rental.controller.webhook.logistics;

import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100CallbackReceipt;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100CallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Kuaidi100TrackingWebhookControllerTest {

    private final Kuaidi100CallbackService callbackService = mock(Kuaidi100CallbackService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new Kuaidi100TrackingWebhookController(callbackService))
            .build();

    @Test
    void acceptsOfficialFormAndReturnsProtocolAck() throws Exception {
        when(callbackService.receive("callback-token", "{\"status\":\"polling\"}", "SIGN"))
                .thenReturn(Kuaidi100CallbackReceipt.success());

        mockMvc.perform(post("/rental/webhooks/kuaidi100/tracking/callback-token")
                        .contentType("application/x-www-form-urlencoded")
                        .param("param", "{\"status\":\"polling\"}")
                        .param("sign", "SIGN"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"result":true,"returnCode":"200","message":"成功"}
                        """));

        verify(callbackService).receive("callback-token", "{\"status\":\"polling\"}", "SIGN");
    }
}
