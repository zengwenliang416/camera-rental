package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ReturnRegistrationRateLimitService {

    private static final int VERIFY_WINDOW_MINUTES = 10;
    private static final int VERIFY_IP_LIMIT = 30;
    private static final int VERIFY_SUBJECT_LIMIT = 10;

    private final RateLimiterRedisDAO rateLimiterRedisDAO;

    public ReturnRegistrationRateLimitService(RateLimiterRedisDAO rateLimiterRedisDAO) {
        this.rateLimiterRedisDAO = rateLimiterRedisDAO;
    }

    public void checkVerification(String clientIp, String orderNo, String mobileLast4,
                                  String machineCode) {
        String ipRef = digest(clientIp == null ? "unknown" : clientIp);
        String subjectRef = digest(normalizeVerificationSubject(orderNo, mobileLast4, machineCode));
        requirePermit("return:verify:ip:" + ipRef, VERIFY_IP_LIMIT, VERIFY_WINDOW_MINUTES);
        requirePermit("return:verify:subject:" + subjectRef,
                VERIFY_SUBJECT_LIMIT, VERIFY_WINDOW_MINUTES);
    }

    public void checkSession(String sessionToken, String action, int countPerMinute) {
        requirePermit("return:session:" + ReturnRegistrationTokenService.rateLimitKey(sessionToken)
                + ":" + action, countPerMinute, 1);
    }

    public static String verificationReference(String orderNo, String mobileLast4,
                                               String machineCode) {
        return digest(normalizeVerificationSubject(orderNo, mobileLast4, machineCode))
                .substring(0, 16);
    }

    private void requirePermit(String key, int count, int minutes) {
        if (!Boolean.TRUE.equals(rateLimiterRedisDAO.tryAcquire(
                key, count, minutes, TimeUnit.MINUTES))) {
            throw new ServiceException(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(),
                    GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getMsg());
        }
    }

    private static String normalizeOrderNo(String orderNo) {
        return orderNo == null ? "" : orderNo.trim();
    }

    private static String normalizeVerificationSubject(String orderNo, String mobileLast4,
                                                       String machineCode) {
        String normalizedOrderNo = normalizeOrderNo(orderNo);
        if (!normalizedOrderNo.isEmpty()) {
            return "order:" + normalizedOrderNo;
        }
        String normalizedMobileLast4 = mobileLast4 == null
                ? "" : mobileLast4.replaceAll("\\D", "");
        if (!normalizedMobileLast4.isEmpty()) {
            return "mobile-last4:" + normalizedMobileLast4;
        }
        String normalizedMachineCode = machineCode == null ? "" : machineCode.trim().toUpperCase();
        return "machine-code:" + normalizedMachineCode;
    }

    private static String digest(String value) {
        return DigestUtil.sha256Hex(value);
    }
}
