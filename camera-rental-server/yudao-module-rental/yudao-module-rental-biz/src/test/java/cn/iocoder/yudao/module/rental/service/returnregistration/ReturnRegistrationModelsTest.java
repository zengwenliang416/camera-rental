package cn.iocoder.yudao.module.rental.service.returnregistration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReturnRegistrationModelsTest {

    @Test
    void serializesAdminShippedDateAsIsoString() throws Exception {
        ReturnRegistrationModels.AdminDetail detail = new ReturnRegistrationModels.AdminDetail(
                1L, "RR-001", 2L, "ORDER-001", "ACCEPTED",
                null, null, null, "SF123", LocalDate.of(2026, 8, 3),
                null, null, null, null, null, null,
                null, null, List.of(), List.of()
        );

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = objectMapper.writeValueAsString(detail);

        assertEquals("2026-08-03", objectMapper.readTree(json).path("shippedDate").asText());
    }

}
