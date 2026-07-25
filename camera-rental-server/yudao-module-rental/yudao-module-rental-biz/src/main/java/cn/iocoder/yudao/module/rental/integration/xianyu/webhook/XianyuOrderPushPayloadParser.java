package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class XianyuOrderPushPayloadParser {

    private static final Set<Integer> ORDER_TYPES = Set.of(1, 2, 3, 4, 7, 8, 9, 10);
    private static final Set<Integer> ORDER_STATUSES = Set.of(11, 12, 21, 22, 23, 24);
    private static final Set<Integer> REFUND_STATUSES = Set.of(0, 1, 2, 3, 4, 5, 6, 8);

    private final ObjectMapper objectMapper;

    public XianyuOrderPushPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public XianyuOrderPushPayload parse(String rawBody) {
        final JsonNode body;
        try {
            body = objectMapper.readTree(rawBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Malformed XianGuanJia order push payload", exception);
        }
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("XianGuanJia order push payload must be an object");
        }
        String sellerId = requiredPositiveInteger(body, "seller_id").asText();
        requiredText(body, "user_name", 256);
        String orderNo = requiredText(body, "order_no", 128);
        int orderType = requiredEnum(body, "order_type", ORDER_TYPES);
        int orderStatus = requiredEnum(body, "order_status", ORDER_STATUSES);
        int refundStatus = requiredEnum(body, "refund_status", REFUND_STATUSES);
        long modifyTime = requiredPositiveInteger(body, "modify_time").longValue();
        long productId = requiredPositiveInteger(body, "product_id").longValue();
        long itemId = requiredPositiveInteger(body, "item_id").longValue();
        return new XianyuOrderPushPayload(sellerId, orderNo, orderType, orderStatus, refundStatus,
                modifyTime, productId, itemId);
    }

    private JsonNode requiredPositiveInteger(JsonNode body, String fieldName) {
        JsonNode value = body.path(fieldName);
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
            throw new IllegalArgumentException("XianGuanJia order push field " + fieldName
                    + " must be a positive integer");
        }
        return value;
    }

    private int requiredEnum(JsonNode body, String fieldName, Set<Integer> allowedValues) {
        JsonNode value = body.path(fieldName);
        if (!value.isIntegralNumber() || !value.canConvertToInt() || !allowedValues.contains(value.intValue())) {
            throw new IllegalArgumentException("XianGuanJia order push field " + fieldName + " is invalid");
        }
        return value.intValue();
    }

    private String requiredText(JsonNode body, String fieldName, int maxLength) {
        JsonNode value = body.path(fieldName);
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().length() > maxLength) {
            throw new IllegalArgumentException("XianGuanJia order push field " + fieldName
                    + " must be a bounded non-empty string");
        }
        return value.textValue();
    }

}
