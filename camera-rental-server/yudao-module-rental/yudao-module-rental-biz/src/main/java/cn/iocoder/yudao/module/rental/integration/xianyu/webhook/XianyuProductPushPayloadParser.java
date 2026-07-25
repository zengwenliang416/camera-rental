package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class XianyuProductPushPayloadParser {

    private static final Set<Integer> PRODUCT_STATUSES = Set.of(-1, 21, 22, 23, 31, 33, 36);
    private static final Set<Integer> PUBLISH_STATUSES = Set.of(-1, 1, 2, 3, 4, 5, 9);
    private static final Set<Integer> ITEM_BIZ_TYPES = Set.of(0, 2, 10, 16, 19, 24, 26, 35);
    private static final long MAX_PRICE_CENTS = 9_999_999_900L;
    private static final int MAX_STOCK = 399_960;

    private final ObjectMapper objectMapper;

    public XianyuProductPushPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public XianyuProductPushPayload parse(String rawBody) {
        final JsonNode body;
        try {
            body = objectMapper.readTree(rawBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Malformed XianGuanJia product push payload", exception);
        }
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("XianGuanJia product push payload must be an object");
        }
        String sellerId = requiredPositiveInteger(body, "seller_id", Long.MAX_VALUE).asText();
        String productId = requiredPositiveInteger(body, "product_id", Long.MAX_VALUE).asText();
        int productStatus = requiredEnum(body, "product_status", PRODUCT_STATUSES);
        int publishStatus = requiredEnum(body, "publish_status", PUBLISH_STATUSES);
        int itemBizType = requiredEnum(body, "item_biz_type", ITEM_BIZ_TYPES);
        long price = requiredPositiveInteger(body, "price", MAX_PRICE_CENTS).longValue();
        int stock = requiredPositiveInteger(body, "stock", MAX_STOCK).intValue();
        requiredText(body, "user_name", 256);
        long modifyTime = requiredPositiveInteger(body, "modify_time", Long.MAX_VALUE).longValue();
        return new XianyuProductPushPayload(sellerId, productId, productStatus, publishStatus,
                itemBizType, price, stock, modifyTime);
    }

    private JsonNode requiredPositiveInteger(JsonNode body, String fieldName, long maxValue) {
        JsonNode value = body.path(fieldName);
        if (!value.isIntegralNumber() || !value.canConvertToLong()
                || value.longValue() <= 0 || value.longValue() > maxValue) {
            throw new IllegalArgumentException("XianGuanJia product push field " + fieldName
                    + " must be a bounded positive integer");
        }
        return value;
    }

    private int requiredEnum(JsonNode body, String fieldName, Set<Integer> allowedValues) {
        JsonNode value = body.path(fieldName);
        if (!value.isIntegralNumber() || !value.canConvertToInt() || !allowedValues.contains(value.intValue())) {
            throw new IllegalArgumentException("XianGuanJia product push field " + fieldName + " is invalid");
        }
        return value.intValue();
    }

    private String requiredText(JsonNode body, String fieldName, int maxLength) {
        JsonNode value = body.path(fieldName);
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().length() > maxLength) {
            throw new IllegalArgumentException("XianGuanJia product push field " + fieldName
                    + " must be a bounded non-empty string");
        }
        return value.textValue();
    }

}
