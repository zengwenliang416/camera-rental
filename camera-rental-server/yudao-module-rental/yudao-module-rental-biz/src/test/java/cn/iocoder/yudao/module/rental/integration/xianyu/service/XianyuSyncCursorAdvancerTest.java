package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianyuSyncCursorAdvancerTest {

    private final XianyuSyncCursorAdvancer advancer = new XianyuSyncCursorAdvancer();
    private final LocalDateTime timestamp = LocalDateTime.of(2026, 7, 23, 12, 0);

    @Test
    void shouldUseExternalOrderIdAsStableTimestampTieBreaker() {
        XianyuSyncCursorDO current = XianyuSyncCursorDO.builder()
                .cursorUpdatedAt(timestamp)
                .cursorExternalId("order-002")
                .build();

        assertFalse(advancer.isStrictlyNewer(current, timestamp, "order-001"));
        assertFalse(advancer.isStrictlyNewer(current, timestamp, "order-002"));
        assertTrue(advancer.isStrictlyNewer(current, timestamp, "order-003"));
        assertTrue(advancer.isStrictlyNewer(current, timestamp.plusSeconds(1), "order-001"));
    }

}
