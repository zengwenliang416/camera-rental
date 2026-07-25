package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class XianyuProductListPageParser {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    public XianyuProductListPage parse(XianyuReadResponse response) {
        JsonNode data = response.payload().path("data");
        JsonNode list = data.path("list");
        if (!data.isObject() || !list.isArray()) {
            throw malformed("XianGuanJia product-list response is missing data.list");
        }
        int count = requiredInt(data, "count");
        int pageNo = requiredInt(data, "page_no");
        int pageSize = requiredInt(data, "page_size");
        if (count < 0 || pageNo < 0 || pageNo > 100 || pageSize < 1 || pageSize > 100) {
            throw malformed("XianGuanJia product-list pagination metadata is outside documented bounds");
        }
        List<XianyuProductListEntry> entries = new ArrayList<>(list.size());
        for (JsonNode item : list) {
            entries.add(new XianyuProductListEntry(requiredIntegralText(item, "product_id"),
                    epochSecondToShanghaiTime(requiredLong(item, "update_time")),
                    requiredInt(item, "spec_type")));
        }
        return new XianyuProductListPage(List.copyOf(entries), count, pageNo, pageSize);
    }

    private int requiredInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw malformed("XianGuanJia product-list response is missing integer " + fieldName);
        }
        return value.intValue();
    }

    private long requiredLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
            throw malformed("XianGuanJia product-list response is missing epoch " + fieldName);
        }
        return value.longValue();
    }

    private String requiredIntegralText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw malformed("XianGuanJia product-list response is missing integer " + fieldName);
        }
        return value.asText();
    }

    private LocalDateTime epochSecondToShanghaiTime(long epochSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), BUSINESS_ZONE);
    }

    private XianyuClientException malformed(String message) {
        return new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE, message);
    }

}
