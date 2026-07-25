package cn.iocoder.yudao.module.rental.controller.webhook.xianyu;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuOrderWebhookService;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuProductWebhookService;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuWebhookReceipt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Tag(name = "闲管家 Webhook")
@RestController
@RequestMapping("/xianyu/webhooks")
public class XianyuOrderWebhookController {

    private static final int MAX_RAW_BODY_BYTES = 65_536;

    private final XianyuOrderWebhookService webhookService;
    private final XianyuProductWebhookService productWebhookService;

    public XianyuOrderWebhookController(XianyuOrderWebhookService webhookService,
                                        XianyuProductWebhookService productWebhookService) {
        this.webhookService = webhookService;
        this.productWebhookService = productWebhookService;
    }

    @PostMapping(value = "/order", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "接收闲管家订单推送")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PermitAll
    public XianyuWebhookReceipt receiveOrder(
            @RequestParam(value = "appid", required = false) String appId,
            @RequestParam(value = "timestamp", required = false) Long timestamp,
            @RequestParam(value = "sign", required = false) String signature,
            HttpServletRequest request) {
        return receive(appId, timestamp, signature, request, webhookService::receive);
    }

    @PostMapping(value = "/product", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "接收闲管家商品推送")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PermitAll
    public XianyuWebhookReceipt receiveProduct(
            @RequestParam(value = "appid", required = false) String appId,
            @RequestParam(value = "timestamp", required = false) Long timestamp,
            @RequestParam(value = "sign", required = false) String signature,
            HttpServletRequest request) {
        return receive(appId, timestamp, signature, request, productWebhookService::receive);
    }

    private XianyuWebhookReceipt receive(String appId, Long timestamp, String signature,
                                         HttpServletRequest request, WebhookReceiver receiver) {
        try {
            if (request.getContentLengthLong() > MAX_RAW_BODY_BYTES) {
                return XianyuWebhookReceipt.fail("请求体过大");
            }
            byte[] body = request.getInputStream().readNBytes(MAX_RAW_BODY_BYTES + 1);
            if (body.length > MAX_RAW_BODY_BYTES) {
                return XianyuWebhookReceipt.fail("请求体过大");
            }
            return receiver.receive(appId, timestamp, signature,
                    new String(body, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return XianyuWebhookReceipt.fail("接收失败");
        }
    }

    @FunctionalInterface
    private interface WebhookReceiver {

        XianyuWebhookReceipt receive(String appId, Long timestamp, String signature, String rawBody);

    }

}
