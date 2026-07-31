package cn.iocoder.yudao.module.rental.service.logistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WaybillPrivacyTest {

    private final WaybillPrivacy privacy = new WaybillPrivacy();

    @Test
    void normalizesWithoutLeakingFormattingDifferences() {
        assertEquals("SF1234567890", privacy.normalize(" sf-123 456 7890 "));
        assertEquals("SF1****7890", privacy.mask("sf1234567890"));
    }

    @Test
    void rejectsMissingOrUnsafeWaybill() {
        assertThrows(RentalLogisticsException.class, () -> privacy.normalize(" "));
        assertThrows(RentalLogisticsException.class, () -> privacy.normalize("SF/1234"));
    }
}
