package cn.iocoder.yudao.module.rental.service.returnregistration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnRegistrationTokenServiceTest {

    private final ReturnRegistrationTokenService service = new ReturnRegistrationTokenService();

    @Test
    void issuesUrlSafe256BitTokensAndStoresStableHashes() {
        ReturnRegistrationTokenService.IssuedToken first = service.issue();
        ReturnRegistrationTokenService.IssuedToken second = service.issue();

        assertTrue(first.plaintext().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(64, first.hash().length());
        assertEquals(first.hash(), service.hash(first.plaintext()));
        assertNotEquals(first.plaintext(), first.hash());
        assertNotEquals(first.hash(), second.hash());
    }

    @Test
    void rateLimitKeyNeverContainsThePlaintextToken() {
        ReturnRegistrationTokenService.IssuedToken token = service.issue();

        String key = ReturnRegistrationTokenService.rateLimitKey(token.plaintext());

        assertEquals(16, key.length());
        assertFalse(key.contains(token.plaintext()));
    }
}
