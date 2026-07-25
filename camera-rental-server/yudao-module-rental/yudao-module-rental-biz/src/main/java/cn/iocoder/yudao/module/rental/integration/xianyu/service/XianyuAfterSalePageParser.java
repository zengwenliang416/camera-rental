package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class XianyuAfterSalePageParser {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final ObjectMapper objectMapper;

    public XianyuAfterSalePageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public XianyuAfterSalePage parse(XianyuReadResponse response) {
        JsonNode data = response.payload().path("data");
        JsonNode list = data.path("list");
        if (!data.isObject() || !list.isArray()) {
            throw malformed("XianGuanJia after-sale response is missing data.list");
        }
        List<XianyuAfterSaleSnapshot> entries = new ArrayList<>(list.size());
        for (JsonNode item : list) {
            entries.add(parseItem(item));
        }
        return new XianyuAfterSalePage(List.copyOf(entries), data.path("has_next_page").asBoolean(false));
    }

    public XianyuAfterSaleSnapshot parseDetail(XianyuReadResponse response) {
        JsonNode data = response.payload().path("data");
        if (!data.isObject()) {
            throw malformed("XianGuanJia after-sale detail response is missing data");
        }
        return parseItem(data);
    }

    private XianyuAfterSaleSnapshot parseItem(JsonNode item) {
        LocalDateTime refundTime = epochSecondToShanghaiTime(optionalLong(item, "refund_time"));
        LocalDateTime applyTime = epochSecondToShanghaiTime(optionalLong(item, "apply_time"));
        LocalDateTime rejectTime = epochSecondToShanghaiTime(optionalLong(item, "reject_time"));
        LocalDateTime sourceUpdatedAt = Stream.of(refundTime, applyTime, rejectTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new XianyuAfterSaleSnapshot(
                requiredText(item, "refund_no"),
                requiredText(item, "order_no"),
                optionalText(item, "refund_status", "UNKNOWN"),
                optionalLong(item, "refund_amount"),
                epochSecondToShanghaiTime(optionalLong(item, "timeout_time")),
                sourceUpdatedAt,
                serialize(item));
    }

    private String serialize(JsonNode item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException exception) {
            throw malformed("XianGuanJia after-sale item cannot be serialized");
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node, fieldName, null);
        if (value == null || value.isBlank()) {
            throw malformed("XianGuanJia after-sale response is missing " + fieldName);
        }
        return value;
    }

    private String optionalText(JsonNode node, String fieldName, String fallback) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        if (value.isTextual()) {
            String text = value.asText();
            return text.isBlank() ? fallback : text;
        }
        if (value.isIntegralNumber() || value.isBoolean()) {
            return value.asText();
        }
        return fallback;
    }

    private Long optionalLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isIntegralNumber() && value.canConvertToLong() ? value.longValue() : null;
    }

    private LocalDateTime epochSecondToShanghaiTime(Long epochSecond) {
        if (epochSecond == null || epochSecond <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), BUSINESS_ZONE);
    }

    private XianyuClientException malformed(String message) {
        return new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE, message);
    }

}
