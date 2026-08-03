package cn.iocoder.yudao.module.rental.service.returnregistration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnSerialNormalizerTest {

    private final ReturnSerialNormalizer normalizer = new ReturnSerialNormalizer();

    @Test
    void normalizesCustomerLabelFormat() {
        assertEquals("P4-01", normalizer.normalize(" p4 － 01 "));
        assertTrue(normalizer.isValid("P4-01"));
    }

    @Test
    void acceptsModelPrefixesWithTwoDigitSequence() {
        assertTrue(normalizer.isValid("P4P-01"));
        assertTrue(normalizer.isValid("DJI-P4P-09"));
    }

    @Test
    void rejectsLegacyLongOrNonTwoDigitCodes() {
        assertFalse(normalizer.isValid("A6-08-4L5H"));
        assertFalse(normalizer.isValid("P4-1"));
        assertFalse(normalizer.isValid("P4-001"));
        assertFalse(normalizer.isValid("P4/01"));
    }
}
