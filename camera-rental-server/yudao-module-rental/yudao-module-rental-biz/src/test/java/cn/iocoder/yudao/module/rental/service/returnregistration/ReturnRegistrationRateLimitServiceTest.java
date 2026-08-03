package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnRegistrationRateLimitServiceTest {

    private final RateLimiterRedisDAO redisDAO = mock(RateLimiterRedisDAO.class);
    private final ReturnRegistrationRateLimitService service =
            new ReturnRegistrationRateLimitService(redisDAO);

    @Test
    void verificationUsesIndependentIpAndSubjectDigestBuckets() {
        when(redisDAO.tryAcquire(contains("return:verify:"), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        service.checkVerification("203.0.113.8", "ORDER-001", "8000", "P4-01");

        verify(redisDAO).tryAcquire(contains("return:verify:ip:"), org.mockito.ArgumentMatchers.eq(30),
                org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES));
        verify(redisDAO).tryAcquire(contains("return:verify:subject:"), org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES));
    }

    @Test
    void deniedBucketReturnsTooManyRequests() {
        when(redisDAO.tryAcquire(contains("return:verify:ip:"), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        assertThrows(ServiceException.class,
                () -> service.checkVerification("203.0.113.8", "", "", "P4-01"));
    }
}
