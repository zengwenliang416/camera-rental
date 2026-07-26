package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuShipmentOcrRespVO;
import cn.iocoder.yudao.module.rental.integration.ocr.ShipmentOcrClient;
import cn.iocoder.yudao.module.rental.integration.ocr.ShipmentOcrResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShipmentOcrServiceTest {

    @Test
    void shouldUseAiRelayResultBeforeLocalFallback() {
        ShipmentOcrClient client = file -> new ShipmentOcrResult(
                "SF5113560342626", "顺丰速运", new BigDecimal("0.91"), "AI_MULTIMODAL");
        ShipmentOcrService service = new ShipmentOcrService(Optional.of(client));

        XianyuShipmentOcrRespVO result = service.extract(new MockMultipartFile(
                "file", "random.jpg", "image/jpeg", "not-an-image".getBytes()));

        assertEquals("SF5113560342626", result.getWaybillNo());
        assertEquals("shunfeng", result.getExpressCode());
        assertEquals("顺丰速运", result.getExpressName());
        assertEquals(new BigDecimal("0.91"), result.getConfidence());
        assertEquals("AI_MULTIMODAL", result.getExtractionSource());
    }

    @Test
    void shouldNotReadWaybillFromOriginalFilename() {
        ShipmentOcrService service = new ShipmentOcrService(Optional.empty());

        XianyuShipmentOcrRespVO result = service.extract(new MockMultipartFile(
                "file",
                "7d2c6f85edb1aa7d015b42bcdeb687c7.jpg",
                "image/jpeg",
                new byte[0]));

        assertNull(result.getWaybillNo());
        assertEquals(BigDecimal.ZERO, result.getConfidence());
        assertEquals("NO_MATCH", result.getExtractionSource());
    }

}
