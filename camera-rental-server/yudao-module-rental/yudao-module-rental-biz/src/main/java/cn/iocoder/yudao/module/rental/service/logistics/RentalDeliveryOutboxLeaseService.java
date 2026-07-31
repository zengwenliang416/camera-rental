package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryOutboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryOutboxMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalAsyncProcessingStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class RentalDeliveryOutboxLeaseService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MAX_MONTHLY_SUBSCRIBE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RentalDeliveryOutboxMapper outboxMapper;
    private final RentalDeliveryMapper deliveryMapper;
    private final RentalLogisticsProviderConfigService configService;
    private final RentalLogisticsProviderCredentialService credentialService;
    private final LogisticsHashing hashing;

    public RentalDeliveryOutboxLeaseService(RentalDeliveryOutboxMapper outboxMapper,
                                            RentalDeliveryMapper deliveryMapper,
                                            RentalLogisticsProviderConfigService configService,
                                            RentalLogisticsProviderCredentialService credentialService,
                                            LogisticsHashing hashing) {
        this.outboxMapper = outboxMapper;
        this.deliveryMapper = deliveryMapper;
        this.configService = configService;
        this.credentialService = credentialService;
        this.hashing = hashing;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<RentalOutboxWorkItem> claim(int requestedLimit) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        List<RentalDeliveryOutboxDO> tasks = outboxMapper.selectClaimableForUpdate(tenantId, now, limit);
        List<RentalOutboxWorkItem> work = new ArrayList<>(tasks.size());
        for (RentalDeliveryOutboxDO task : tasks) {
            String token = UUID.randomUUID().toString();
            task.setProcessingStatus(RentalAsyncProcessingStatusEnum.PROCESSING.name());
            task.setProcessingToken(token);
            task.setLeaseUntil(now.plusMinutes(5));
            outboxMapper.updateById(task);
            work.add(prepare(tenantId, task, token, now));
        }
        return work;
    }

    private RentalOutboxWorkItem prepare(Long tenantId, RentalDeliveryOutboxDO task, String token,
                                         LocalDateTime now) {
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndIdForUpdate(tenantId, task.getDeliveryId());
        if (delivery == null) {
            return skipped(tenantId, task, token, null, "DELIVERY_NOT_FOUND");
        }
        RentalDeliveryOutboxEventTypeEnum eventType =
                RentalDeliveryOutboxEventTypeEnum.valueOf(task.getEventType());
        if (!StringUtils.hasText(delivery.getProviderCode())
                || !StringUtils.hasText(delivery.getProviderCarrierCode())) {
            return skipped(tenantId, task, token, delivery, "MAPPING_REQUIRED");
        }
        RentalLogisticsProviderConfigDO config = configService.get(delivery.getProviderCode());
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return skipped(tenantId, task, token, delivery, "PROVIDER_DISABLED");
        }
        RentalLogisticsProviderCredentialDO credential = credentialService.resolveForDelivery(delivery);
        if (credential == null) {
            return skipped(tenantId, task, token, delivery, "PROVIDER_CREDENTIAL_REQUIRED");
        }
        delivery.setProviderCredentialId(credential.getId());
        String callbackUrl = null;
        String callbackSalt = null;
        if (eventType == RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE) {
            if (!Boolean.TRUE.equals(config.getSubscribeEnabled())) {
                return skipped(tenantId, task, token, delivery, "SUBSCRIPTION_DISABLED");
            }
            String subscribeSkip = reserveSubscribeAttempt(delivery, now);
            if (subscribeSkip != null) {
                return skipped(tenantId, task, token, delivery, subscribeSkip);
            }
            ensureCallbackIdentity(delivery);
            if (!StringUtils.hasText(config.getCallbackBaseUrl())) {
                return skipped(tenantId, task, token, delivery, "CALLBACK_BASE_URL_REQUIRED");
            }
            callbackUrl = stripTrailingSlash(config.getCallbackBaseUrl())
                    + "/rental/webhooks/kuaidi100/tracking/" + delivery.getCallbackToken();
            callbackSalt = delivery.getCallbackSalt();
        } else {
            if (!Boolean.TRUE.equals(config.getQueryEnabled())) {
                return skipped(tenantId, task, token, delivery, "QUERY_DISABLED");
            }
            if (delivery.getNextQueryAllowedAt() != null
                    && delivery.getNextQueryAllowedAt().isAfter(now)) {
                return skipped(tenantId, task, token, delivery, "QUERY_THROTTLED");
            }
            delivery.setNextQueryAllowedAt(now.plusSeconds(
                    configService.minimumQueryIntervalSeconds(delivery.getProviderCode())));
        }
        deliveryMapper.updateById(delivery);
        return new RentalOutboxWorkItem(tenantId, task.getId(), token, delivery.getId(), credential.getId(), eventType,
                delivery.getProviderCode(), delivery.getProviderCarrierCode(), delivery.getWaybillNo(),
                delivery.getTrackingPhone(), callbackUrl, callbackSalt, null);
    }

    private String reserveSubscribeAttempt(RentalDeliveryDO delivery, LocalDateTime now) {
        if (delivery.getNextSubscribeAllowedAt() != null
                && delivery.getNextSubscribeAllowedAt().isAfter(now)) {
            return "SUBSCRIBE_THROTTLED";
        }
        String month = YearMonth.from(now).toString();
        int count = month.equals(delivery.getSubscribeMonth()) && delivery.getSubscribeCount() != null
                ? delivery.getSubscribeCount() : 0;
        if (count >= MAX_MONTHLY_SUBSCRIBE_ATTEMPTS) {
            return "SUBSCRIBE_MONTHLY_LIMIT";
        }
        delivery.setSubscribeMonth(month);
        delivery.setSubscribeCount(count + 1);
        delivery.setNextSubscribeAllowedAt(now.plusHours(1));
        return null;
    }

    private void ensureCallbackIdentity(RentalDeliveryDO delivery) {
        if (!StringUtils.hasText(delivery.getCallbackToken())) {
            String token = randomSecret(24);
            delivery.setCallbackToken(token);
            delivery.setCallbackTokenHash(hashing.sha256(token));
        }
        if (!StringUtils.hasText(delivery.getCallbackSalt())) {
            delivery.setCallbackSalt(randomSecret(24));
        }
    }

    private String randomSecret(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private RentalOutboxWorkItem skipped(Long tenantId, RentalDeliveryOutboxDO task, String token,
                                         RentalDeliveryDO delivery, String code) {
        return new RentalOutboxWorkItem(tenantId, task.getId(), token, task.getDeliveryId(), null,
                RentalDeliveryOutboxEventTypeEnum.valueOf(task.getEventType()),
                delivery == null ? null : delivery.getProviderCode(),
                delivery == null ? null : delivery.getProviderCarrierCode(),
                null, null, null, null, code);
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
