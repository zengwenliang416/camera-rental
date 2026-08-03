package cn.iocoder.yudao.module.rental.service.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalDeviceCodeTest {

    @Test
    void formatsStableTwoDigitCodes() {
        assertEquals("P4-01", RentalDeviceCode.format("p4", 1));
        assertEquals("DJI-P4P-09", RentalDeviceCode.format("DJI-P4P", 9));
        assertTrue(RentalDeviceCode.isValid("P4P-99"));
    }

    @Test
    void rejectsSequenceBeyondTwoDigits() {
        assertThrows(IllegalArgumentException.class, () -> RentalDeviceCode.format("P4", 100));
    }
}
