package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XianyuOrderSyncWindowTest {

    @Test
    void shouldSerializeDocumentedFixedUpdateTimeRange() {
        XianyuOrderSyncWindow window = new XianyuOrderSyncWindow(
                LocalDateTime.of(2026, 7, 22, 0, 0),
                LocalDateTime.of(2026, 7, 23, 0, 0), 1, 50);

        var request = window.toRequestBody(new ObjectMapper(), 88L);

        assertEquals(88L, request.path("authorize_id").longValue());
        assertEquals(1, request.path("page_no").intValue());
        assertEquals(50, request.path("page_size").intValue());
        assertEquals(2, request.path("update_time").size());
        assertEquals(1784649600L, request.path("update_time").get(0).longValue());
        assertEquals(1784736000L, request.path("update_time").get(1).longValue());
    }

    @Test
    void shouldRejectAnUnsafeRangeOrPageBounds() {
        assertThrows(IllegalArgumentException.class, () -> new XianyuOrderSyncWindow(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0), 1, 50));
        assertThrows(IllegalArgumentException.class, () -> new XianyuOrderSyncWindow(
                LocalDateTime.of(2026, 7, 22, 0, 0), LocalDateTime.of(2026, 7, 23, 0, 0), 101, 50));
    }

}
