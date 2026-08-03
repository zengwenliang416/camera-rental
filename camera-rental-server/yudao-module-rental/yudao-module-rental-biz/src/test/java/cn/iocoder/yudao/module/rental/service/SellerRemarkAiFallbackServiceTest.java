package cn.iocoder.yudao.module.rental.service;

import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SellerRemarkAiFallbackServiceTest {

    private final SellerRemarkAiFallbackService service = new SellerRemarkAiFallbackService(
            mock(AiModelService.class), new ObjectMapper());
    private final AiModelDO model = AiModelDO.builder().model("gpt-5.6-luna").build();

    @Test
    void acceptsHighConfidenceStructuredLogisticsDecision() {
        String json = """
                {"sourceMode":"LOGISTICS_DERIVED","shipDate":"2026-08-03",
                 "receiveDate":"2026-08-04","returnDate":"2026-08-08",
                 "billableStartDate":null,"billableEndDate":null,"confidence":0.995,
                 "ambiguous":false,
                 "evidenceFields":["shipDate","receiveDate","returnDate"]}""";

        SellerRemarkResolution result = service.validateDecision(
                json, LocalDate.of(2026, 8, 2), model).orElseThrow();

        assertEquals("AI", result.source());
        assertEquals(LocalDate.of(2026, 8, 5), result.period().billableStartDate());
        assertEquals(LocalDate.of(2026, 8, 8), result.period().billableEndDate());
        assertEquals("gpt-5.6-luna", result.model());
    }

    @Test
    void rejectsLowConfidenceOrAmbiguousDecision() {
        String json = """
                {"sourceMode":"LOGISTICS_DERIVED","shipDate":"2026-08-03",
                 "receiveDate":"2026-08-04","returnDate":"2026-08-08",
                 "confidence":0.90,"ambiguous":false,
                 "evidenceFields":["shipDate","receiveDate","returnDate"]}""";

        Optional<SellerRemarkResolution> result = service.validateDecision(
                json, LocalDate.of(2026, 8, 2), model);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsDatesBeforeOrderOrUnsupportedEvidence() {
        String json = """
                {"sourceMode":"EXPLICIT_PERIOD","shipDate":"2026-08-01",
                 "receiveDate":"2026-08-04","returnDate":"2026-08-08",
                 "billableStartDate":"2026-08-05","billableEndDate":"2026-08-08",
                 "confidence":0.999,"ambiguous":false,
                 "evidenceFields":["receiveDate","returnDate"]}""";

        Optional<SellerRemarkResolution> result = service.validateDecision(
                json, LocalDate.of(2026, 8, 2), model);

        assertTrue(result.isEmpty());
    }

}
