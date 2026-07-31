package cn.iocoder.yudao.module.rental.service.logistics;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RentalAsyncRetryPolicy {

    private static final int MAX_RETRIES = 6;

    public boolean exhausted(int retryCount) {
        return retryCount >= MAX_RETRIES;
    }

    public Duration delay(int retryCount) {
        long minutes = Math.min(360, 1L << Math.min(Math.max(retryCount, 0), 9));
        return Duration.ofMinutes(minutes);
    }
}
