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

@Component
public class XianyuProductDetailPayloadParser {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ObjectMapper objectMapper;
    private final XianyuChannelIdentifierNormalizer identifierNormalizer;

    public XianyuProductDetailPayloadParser(ObjectMapper objectMapper,
                                            XianyuChannelIdentifierNormalizer identifierNormalizer) {
        this.objectMapper = objectMapper;
        this.identifierNormalizer = identifierNormalizer;
    }

    public XianyuProductSnapshot parse(String rawPayload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException exception) {
            throw malformed("XianGuanJia product payload is malformed", exception);
        }
        if (root.has("code") && (!root.path("code").canConvertToInt() || root.path("code").intValue() != 0)) {
            throw malformed("XianGuanJia product payload is not a successful response");
        }
        JsonNode detail = root.has("data") ? root.path("data") : root;
        if (!detail.isObject()) {
            throw malformed("XianGuanJia product payload data is missing");
        }
        return new XianyuProductSnapshot(
                requiredIdentifier(detail, "product_id"),
                parsePublishedItems(detail.path("publish_shop")),
                optionalText(detail, "title"),
                optionalText(detail, "channel_cat_id"),
                optionalIntegralText(detail, "product_status", "UNKNOWN"),
                epochSecondToShanghaiTime(optionalLong(detail, "update_time"))
        );
    }

    private List<XianyuPublishedItem> parsePublishedItems(JsonNode publishShops) {
        if (publishShops == null || publishShops.isMissingNode() || publishShops.isNull()) {
            return List.of();
        }
        if (!publishShops.isArray()) {
            throw malformed("XianGuanJia product payload field publish_shop is invalid");
        }
        List<XianyuPublishedItem> items = new ArrayList<>(publishShops.size());
        for (JsonNode publishShop : publishShops) {
            String userName = requiredText(publishShop, "user_name");
            String itemId = optionalIdentifier(publishShop, "item_id");
            Integer status = optionalInteger(publishShop, "status");
            items.add(new XianyuPublishedItem(userName, itemId, status));
        }
        return List.copyOf(items);
    }

    private String requiredIdentifier(JsonNode node, String fieldName) {
        try {
            return identifierNormalizer.normalizeRequired(node, fieldName);
        } catch (IllegalArgumentException exception) {
            throw malformed("XianGuanJia product payload has invalid " + fieldName, exception);
        }
    }

    private String optionalIdentifier(JsonNode node, String fieldName) {
        try {
            return identifierNormalizer.normalizeOptional(node, fieldName);
        } catch (IllegalArgumentException exception) {
            throw malformed("XianGuanJia product payload has invalid " + fieldName, exception);
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node, fieldName);
        if (value == null || value.isBlank()) {
            throw malformed("XianGuanJia product payload is missing " + fieldName);
        }
        return value.trim();
    }

    private String optionalIntegralText(JsonNode node, String fieldName, String fallback) {
        JsonNode value = node.path(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw malformed("XianGuanJia product payload field " + fieldName + " is not an integer");
        }
        return value.asText();
    }

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value != null && value.isValueNode() && !value.isNull() ? value.asText() : null;
    }

    private Long optionalLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw malformed("XianGuanJia product payload field " + fieldName + " is not an integer");
        }
        return value.longValue();
    }

    private Integer optionalInteger(JsonNode node, String fieldName) {
        Long value = optionalLong(node, fieldName);
        return value == null ? null : Math.toIntExact(value);
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
