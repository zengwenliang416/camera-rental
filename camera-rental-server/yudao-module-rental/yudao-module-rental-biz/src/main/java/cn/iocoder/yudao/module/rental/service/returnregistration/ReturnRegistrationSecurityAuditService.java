package cn.iocoder.yudao.module.rental.service.returnregistration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReturnRegistrationSecurityAuditService {

    public void record(String action, String token, Long registrationId, String result) {
        log.info("[return-registration][security-audit] action={} tokenRef={} registrationId={} result={}",
                action, ReturnRegistrationTokenService.rateLimitKey(token), registrationId, result);
    }

    public void recordVerification(String orderNo, String mobileLast4, String machineCode,
                                   Long registrationId, String result) {
        log.info("[return-registration][security-audit] action=VERIFY verificationRef={} registrationId={} result={}",
                ReturnRegistrationRateLimitService.verificationReference(
                        orderNo, mobileLast4, machineCode),
                registrationId, result);
    }
}
