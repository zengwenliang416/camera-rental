package cn.iocoder.yudao.module.rental.service.logistics;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalAsyncRetryPolicyTest {

    private final RentalAsyncRetryPolicy policy = new RentalAsyncRetryPolicy();

    @Test
    void capsBackoffAndRetryCount() {
        assertEquals(Duration.ofMinutes(2), policy.delay(1));
        assertEquals(Duration.ofMinutes(360), policy.delay(20));
        assertTrue(policy.exhausted(6));
    }
}
