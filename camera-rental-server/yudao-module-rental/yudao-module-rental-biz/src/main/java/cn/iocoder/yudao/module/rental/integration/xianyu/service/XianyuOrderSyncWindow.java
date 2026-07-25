package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Immutable fixed order-list query window. The end is captured before any page read.
 */
public record XianyuOrderSyncWindow(LocalDateTime start, LocalDateTime end, int pageNo, int pageSize) {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    static final int MAX_PAGE_NUMBER = 100;
    static final int MAX_PAGE_SIZE = 100;

    public XianyuOrderSyncWindow {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("XianGuanJia order sync window start must be before end");
        }
        if (end.isAfter(start.plusMonths(6))) {
            throw new IllegalArgumentException("XianGuanJia order sync window must not exceed six months");
        }
        if (pageNo < 1 || pageNo > MAX_PAGE_NUMBER) {
            throw new IllegalArgumentException("XianGuanJia order sync page number must be between 1 and 100");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("XianGuanJia order sync page size must be between 1 and 100");
        }
    }

    public ObjectNode toRequestBody(ObjectMapper objectMapper, Long authorizeId) {
        Objects.requireNonNull(authorizeId, "authorizeId");
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode updateTime = body.putArray("update_time");
        updateTime.add(start.atZone(BUSINESS_ZONE).toEpochSecond());
        updateTime.add(end.atZone(BUSINESS_ZONE).toEpochSecond());
        body.put("authorize_id", authorizeId);
        body.put("page_no", pageNo);
        body.put("page_size", pageSize);
        return body;
    }

}
