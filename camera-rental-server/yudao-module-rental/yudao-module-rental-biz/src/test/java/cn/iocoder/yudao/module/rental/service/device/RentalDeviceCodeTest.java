package cn.iocoder.yudao.module.rental.service.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalDeviceCodeTest {

    @Test
    void formatsStableTwoAndThreeDigitCodes() {
        assertEquals("P4-01", RentalDeviceCode.format("p4", 1));
        assertEquals("DJI-P4P-09", RentalDeviceCode.format("DJI-P4P", 9));
        assertTrue(RentalDeviceCode.isValid("P4P-99"));
        assertEquals("P4P-100", RentalDeviceCode.format("P4P", 100));
        assertEquals("P4P-999", RentalDeviceCode.format("P4P", 999));
    }

    @Test
    void rejectsSequenceBeyondThreeDigits() {
        assertThrows(IllegalArgumentException.class, () -> RentalDeviceCode.format("P4", 1000));
        assertThrows(IllegalArgumentException.class, () -> RentalDeviceCode.format("P4", 0));
        assertThrows(IllegalArgumentException.class, () -> RentalDeviceCode.format("P4", -1));
        assertFalse(RentalDeviceCode.isValid("P4-001"));
    }
}
