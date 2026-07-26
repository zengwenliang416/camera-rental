package cn.iocoder.yudao.module.rental.integration.ocr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yudao.ai.shipment-ocr")
@Data
public class ShipmentOcrProperties {

    /**
     * Enables the OpenAI-compatible multimodal OCR adapter. Manual entry and
     * local barcode fallback remain available when disabled or unavailable.
     */
    private boolean enable = false;

    /**
     * OpenAI-compatible base URL. Use the provider's /v1 URL when required.
     */
    private String baseUrl;

    private String apiKey;

    private String model;

}
