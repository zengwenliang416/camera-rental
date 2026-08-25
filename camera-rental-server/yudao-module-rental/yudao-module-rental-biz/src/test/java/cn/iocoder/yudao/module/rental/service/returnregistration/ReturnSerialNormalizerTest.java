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
    void acceptsCurrentReturnModelPrefixesAndExplicitStandCode() {
        String[] prefixes = {
                "360", "NANO", "A5", "A6", "P3", "P4", "P4P",
                "ACE", "X5", "GT", "G3",
                "X300P", "X200U", "X300U",
                "XT5", "XT50", "XS20", "X100VI",
                "R50", "G12", "G7X2",
                "GR3X", "GR4",
                "支架"
        };
        for (String prefix : prefixes) {
            assertTrue(normalizer.isValid(prefix + "-01"), prefix);
        }
        assertFalse(normalizer.isValid("相机-01"));
    }

    @Test
    void rejectsLegacyLongOrNonTwoDigitCodes() {
        assertFalse(normalizer.isValid("A6-08-4L5H"));
        assertFalse(normalizer.isValid("P4-1"));
        assertFalse(normalizer.isValid("P4-001"));
        assertFalse(normalizer.isValid("P4/01"));
    }
}
