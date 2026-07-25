package cn.iocoder.yudao.module.rental.integration.xianyu.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Raw response is retained only for subsequent restricted persistence work.
 */
public record XianyuReadResponse(
        int httpStatus,
        Integer remoteCode,
        JsonNode payload,
        String rawBody) {
}
