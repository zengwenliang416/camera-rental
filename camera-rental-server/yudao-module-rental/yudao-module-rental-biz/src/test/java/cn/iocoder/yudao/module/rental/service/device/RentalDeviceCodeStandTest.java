package cn.iocoder.yudao.module.rental.service.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalDeviceCodeStandTest {

    @Test
    void supportsTheExplicitStandPrefix() {
        assertEquals("支架-01", RentalDeviceCode.format("支架", 1));
        assertTrue(RentalDeviceCode.isValid("支架-99"));
    }

}
