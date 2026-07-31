package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.service.logistics.LogisticsHashing;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryInboxService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class Kuaidi100CallbackService {

    private static final int MAX_PARAM_BYTES = 262_144;

    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryInboxService inboxService;
    private final LogisticsHashing hashing;
    private final Kuaidi100Signer signer;

    public Kuaidi100CallbackService(RentalDeliveryMapper deliveryMapper,
                                    RentalDeliveryInboxService inboxService,
                                    LogisticsHashing hashing,
                                    Kuaidi100Signer signer) {
        this.deliveryMapper = deliveryMapper;
        this.inboxService = inboxService;
        this.hashing = hashing;
        this.signer = signer;
    }

    public Kuaidi100CallbackReceipt receive(String callbackToken, String param, String signature) {
        if (!StringUtils.hasText(callbackToken) || !StringUtils.hasText(param)
                || !StringUtils.hasText(signature)
                || param.getBytes(StandardCharsets.UTF_8).length > MAX_PARAM_BYTES) {
            return Kuaidi100CallbackReceipt.failure();
        }
        List<RentalDeliveryDO> candidates = TenantUtils.executeIgnore(
                () -> deliveryMapper.selectCallbackCandidatesByTokenHash(hashing.sha256(callbackToken)));
        RentalDeliveryDO delivery = findUniqueTokenMatch(candidates, callbackToken);
        if (delivery == null || !signer.verifyCallback(param, delivery.getCallbackSalt(), signature)) {
            return Kuaidi100CallbackReceipt.failure();
        }
        try {
            TenantUtils.execute(delivery.getTenantId(), () -> inboxService.accept(
                    Kuaidi100LogisticsProvider.PROVIDER_CODE, delivery.getId(), null,
                    hashing.sha256(param), param));
            return Kuaidi100CallbackReceipt.success();
        } catch (RuntimeException exception) {
            return Kuaidi100CallbackReceipt.failure();
        }
    }

    private RentalDeliveryDO findUniqueTokenMatch(List<RentalDeliveryDO> candidates, String callbackToken) {
        byte[] callbackTokenBytes = callbackToken.getBytes(StandardCharsets.UTF_8);
        RentalDeliveryDO matched = null;
        int matchCount = 0;
        for (RentalDeliveryDO candidate : candidates) {
            if (!StringUtils.hasText(candidate.getCallbackToken())) {
                continue;
            }
            boolean matches = MessageDigest.isEqual(
                    callbackTokenBytes, candidate.getCallbackToken().getBytes(StandardCharsets.UTF_8));
            if (matches) {
                matched = candidate;
                matchCount++;
            }
        }
        return matchCount == 1 ? matched : null;
    }
}
