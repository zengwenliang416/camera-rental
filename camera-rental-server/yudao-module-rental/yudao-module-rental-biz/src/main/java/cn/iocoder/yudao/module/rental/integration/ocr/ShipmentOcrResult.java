package cn.iocoder.yudao.module.rental.integration.ocr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentOcrResult {

    private String waybillNo;
    private String expressName;
    private BigDecimal confidence;
    private String source;

    public static ShipmentOcrResult empty(String source) {
        return new ShipmentOcrResult(null, null, BigDecimal.ZERO, source);
    }

}
