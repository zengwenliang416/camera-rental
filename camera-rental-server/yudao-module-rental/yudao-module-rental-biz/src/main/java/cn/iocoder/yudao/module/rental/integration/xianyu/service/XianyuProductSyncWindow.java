package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.ZoneId;

public record XianyuProductSyncWindow(
        LocalDateTime start,
        LocalDateTime end,
        int pageNo,
        int pageSize
) {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    public XianyuProductSyncWindow {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Product sync window must have start < end");
        }
        if (pageNo < 1 || pageNo > 100 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Product sync page_no/page_size must be within documented bounds");
        }
    }

    public ObjectNode toRequestBody(ObjectMapper objectMapper) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("update_time")
                .add(start.atZone(BUSINESS_ZONE).toEpochSecond())
                .add(end.atZone(BUSINESS_ZONE).toEpochSecond());
        body.put("page_no", pageNo);
        body.put("page_size", pageSize);
        return body;
    }

}
