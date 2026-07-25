package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeSerializer;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncReqVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract test for the admin order-sync request body against the shipped Yudao
 * {@link TimestampLocalDateTimeDeserializer}.
 *
 * <p>Production admin UI must POST {@code windowStart}/{@code windowEnd} as epoch millis
 * (Element Plus {@code value-format="x"} + {@code toEpochMillis}). This test locks that
 * codec contract; it does not re-implement the deserializer.
 */
class XianyuOrderSyncReqVOJacksonTest {

    private final ObjectMapper objectMapper = createYudaoStyleMapper();

    @Test
    void shouldDeserializeWindowBoundsFromNumericEpochMillis() throws Exception {
        long startMillis = 1_720_000_000_000L;
        long endMillis = 1_720_003_600_000L;
        String json = """
                {"shopId":1,"windowStart":%d,"windowEnd":%d,"pageNo":1,"pageSize":20}
                """.formatted(startMillis, endMillis);

        XianyuOrderSyncReqVO vo = objectMapper.readValue(json, XianyuOrderSyncReqVO.class);

        assertEquals(1L, vo.getShopId());
        assertEquals(toLocalDateTime(startMillis), vo.getWindowStart());
        assertEquals(toLocalDateTime(endMillis), vo.getWindowEnd());
        assertEquals(1, vo.getPageNo());
        assertEquals(20, vo.getPageSize());
    }

    @Test
    void shouldDeserializeWindowBoundsFromNumericStringEpochMillis() throws Exception {
        // Element Plus value-format="x" often yields a string of digits before JSON number coercion.
        long startMillis = 1_720_000_000_000L;
        long endMillis = 1_720_003_600_000L;
        String json = """
                {"shopId":1,"windowStart":"%d","windowEnd":"%d","pageNo":1,"pageSize":20}
                """.formatted(startMillis, endMillis);

        XianyuOrderSyncReqVO vo = objectMapper.readValue(json, XianyuOrderSyncReqVO.class);

        assertEquals(toLocalDateTime(startMillis), vo.getWindowStart());
        assertEquals(toLocalDateTime(endMillis), vo.getWindowEnd());
    }

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private static ObjectMapper createYudaoStyleMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("TimestampSupportModule");
        module.addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE);
        module.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        mapper.registerModule(module);
        return mapper;
    }

}
