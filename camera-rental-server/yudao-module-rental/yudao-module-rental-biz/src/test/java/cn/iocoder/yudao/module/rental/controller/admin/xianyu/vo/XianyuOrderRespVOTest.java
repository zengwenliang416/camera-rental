package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XianyuOrderRespVOTest {

    @Test
    void serializesBusinessDatesAsIsoStrings() throws Exception {
        XianyuOrderRespVO value = new XianyuOrderRespVO();
        value.setBillableStartDate(LocalDate.of(2026, 8, 8));
        value.setBillableEndDate(LocalDate.of(2026, 8, 17));
        value.setOccupyStartDate(LocalDate.of(2026, 8, 6));
        value.setOccupyEndDateExclusive(LocalDate.of(2026, 8, 18));

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = objectMapper.writeValueAsString(value);

        assertEquals("2026-08-08", objectMapper.readTree(json).path("billableStartDate").asText());
        assertEquals("2026-08-17", objectMapper.readTree(json).path("billableEndDate").asText());
        assertEquals("2026-08-06", objectMapper.readTree(json).path("occupyStartDate").asText());
        assertEquals("2026-08-18", objectMapper.readTree(json).path("occupyEndDateExclusive").asText());
    }

}
