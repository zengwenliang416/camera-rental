package cn.iocoder.yudao.module.rental.service.returnregistration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnSerialNormalizerTest {

    private final ReturnSerialNormalizer normalizer = new ReturnSerialNormalizer();

    @Test
    void normalizesCustomerLabelFormat() {
        assertEquals("A6-08-4L5H", normalizer.normalize(" a6 － 08 — 4l5h "));
        assertTrue(normalizer.isValid("A6-08-4L5H"));
    }

    @Test
    void rejectsSerialsWithoutGroupedDashFormat() {
        assertFalse(normalizer.isValid("A6084L5H"));
        assertFalse(normalizer.isValid("A6-08-"));
    }
}
