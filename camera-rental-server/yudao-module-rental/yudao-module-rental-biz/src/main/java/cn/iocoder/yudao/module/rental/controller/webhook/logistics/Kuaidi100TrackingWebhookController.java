package cn.iocoder.yudao.module.rental.controller.webhook.logistics;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100CallbackReceipt;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100CallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "快递100物流回调")
@RestController
@RequestMapping("/rental/webhooks/kuaidi100")
public class Kuaidi100TrackingWebhookController {

    private final Kuaidi100CallbackService callbackService;

    public Kuaidi100TrackingWebhookController(Kuaidi100CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @PostMapping(value = "/tracking/{callbackToken}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "接收快递100物流轨迹推送")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PermitAll
    public Kuaidi100CallbackReceipt receive(@PathVariable String callbackToken,
                                            @RequestParam("param") String param,
                                            @RequestParam("sign") String signature) {
        return callbackService.receive(callbackToken, param, signature);
    }
}
