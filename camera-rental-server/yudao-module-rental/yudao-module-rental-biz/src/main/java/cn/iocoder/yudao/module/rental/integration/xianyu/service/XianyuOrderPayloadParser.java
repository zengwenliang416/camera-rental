package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses order-detail response into normalized columns + full {@code data} JSON.
 */
@Component
public class XianyuOrderPayloadParser {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ObjectMapper objectMapper;

    public XianyuOrderPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public XianyuOrderSnapshot parse(String rawPayload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException exception) {
            throw malformed("XianGuanJia order payload is malformed", exception);
        }
        if (root.has("code") && (!root.path("code").canConvertToInt() || root.path("code").intValue() != 0)) {
            throw malformed("XianGuanJia order payload is not a successful response");
        }
        JsonNode detail = root.has("data") ? root.path("data") : root;
        if (!detail.isObject()) {
            throw malformed("XianGuanJia order payload data is missing");
        }
        JsonNode goods = detail.path("goods");
        String detailJson;
        String goodsJson = null;
        try {
            detailJson = objectMapper.writeValueAsString(detail);
            if (goods.isObject()) {
                goodsJson = objectMapper.writeValueAsString(goods);
            }
        } catch (JsonProcessingException exception) {
            throw malformed("XianGuanJia order payload cannot be re-serialized", exception);
        }
        return new XianyuOrderSnapshot(
                requiredText(detail, "order_no"),
                firstText(goods, "product_id", "item_id"),
                optionalText(goods, "sku_id"),
                optionalText(detail, "order_status", "UNKNOWN"),
                optionalLong(detail, "pay_amount"),
                optionalText(detail, "seller_remark"),
                epochSecondToShanghaiTime(optionalLong(detail, "create_time")),
                epochSecondToShanghaiTime(optionalLong(detail, "update_time")),
                detailJson,
                optionalText(detail, "receiver_name"),
                optionalText(detail, "receiver_mobile"),
                composeAddress(detail),
                optionalInt(detail, "order_type"),
                epochSecondToShanghaiTime(optionalLong(detail, "order_time")),
                optionalLong(detail, "total_amount"),
                optionalText(detail, "pay_no"),
                epochSecondToShanghaiTime(optionalLong(detail, "pay_time")),
                optionalInt(detail, "refund_status"),
                optionalLong(detail, "refund_amount"),
                epochSecondToShanghaiTime(optionalLong(detail, "refund_time")),
                optionalText(detail, "waybill_no"),
                optionalText(detail, "express_code"),
                optionalText(detail, "express_name"),
                optionalLong(detail, "express_fee"),
                optionalInt(detail, "consign_type"),
                epochSecondToShanghaiTime(optionalLong(detail, "consign_time")),
                epochSecondToShanghaiTime(optionalLong(detail, "confirm_time")),
                optionalText(detail, "cancel_reason"),
                epochSecondToShanghaiTime(optionalLong(detail, "cancel_time")),
                optionalText(detail, "buyer_nick"),
                optionalText(detail, "seller_name"),
                optionalText(goods, "title"),
                optionalInt(goods, "quantity"),
                optionalLong(goods, "price"),
                goodsJson,
                optionalLong(detail, "xyb_seller_amount"),
                optionalBoolean(detail, "is_tax_included"),
                optionalInt(detail, "idle_biz_type"),
                optionalInt(detail, "pin_group_status")
        );
    }

    private String composeAddress(JsonNode detail) {
        List<String> parts = new ArrayList<>(5);
        for (String field : List.of("prov_name", "city_name", "area_name", "town_name", "address")) {
            String part = optionalText(detail, field);
            if (part != null && !part.isBlank()) {
                parts.add(part.trim());
            }
        }
        return parts.isEmpty() ? null : String.join("", parts);
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node, fieldName);
        if (value == null || value.isBlank()) {
            throw malformed("XianGuanJia order payload is missing " + fieldName);
        }
        return value;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = optionalText(node, fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String optionalText(JsonNode node, String fieldName) {
        return optionalText(node, fieldName, null);
    }

    private String optionalText(JsonNode node, String fieldName, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        JsonNode value = node.path(fieldName);
        if (!value.isValueNode() || value.isNull()) {
            return fallback;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? fallback : text;
    }

    private Long optionalLong(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.path(fieldName);
        if (value == null || !value.isValueNode() || value.isNull()) {
            return null;
        }
        if (!value.canConvertToLong()) {
            throw malformed("XianGuanJia order payload field " + fieldName + " is not an integer");
        }
        return value.longValue();
    }

    private Integer optionalInt(JsonNode node, String fieldName) {
        Long value = optionalLong(node, fieldName);
        return value == null ? null : value.intValue();
    }

    private Boolean optionalBoolean(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.path(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isIntegralNumber()) {
            return value.intValue() != 0;
        }
        if (value.isTextual()) {
            return Boolean.parseBoolean(value.asText());
        }
        return null;
    }

    private LocalDateTime epochSecondToShanghaiTime(Long epochSecond) {
        return epochSecond == null || epochSecond <= 0 ? null
                : LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), BUSINESS_ZONE);
    }

    private XianyuClientException malformed(String message) {
        return new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE, message);
    }

    private XianyuClientException malformed(String message, Exception cause) {
        return new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE, message, cause);
    }

}
