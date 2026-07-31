package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsProviderCredentialMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class RentalLogisticsProviderCredentialService {

    private final RentalLogisticsProviderCredentialMapper credentialMapper;

    public RentalLogisticsProviderCredentialService(
            RentalLogisticsProviderCredentialMapper credentialMapper) {
        this.credentialMapper = credentialMapper;
    }

    public RentalLogisticsProviderCredentialDO get(Long credentialId) {
        if (credentialId == null) {
            return null;
        }
        return credentialMapper.selectByTenantIdAndId(
                TenantContextHolder.getRequiredTenantId(), credentialId);
    }

    public RentalLogisticsProviderCredentialDO resolveForDelivery(RentalDeliveryDO delivery) {
        RentalLogisticsProviderCredentialDO bound = get(delivery.getProviderCredentialId());
        if (isUsable(bound, delivery.getProviderCode())) {
            return bound;
        }
        List<RentalLogisticsProviderCredentialDO> credentials =
                credentialMapper.selectListByProvider(
                        TenantContextHolder.getRequiredTenantId(), delivery.getProviderCode())
                        .stream()
                        .filter(candidate -> isUsable(candidate, delivery.getProviderCode()))
                        .toList();
        if (credentials.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(Long.hashCode(delivery.getId()), credentials.size());
        return credentials.get(index);
    }

    public boolean hasUsableCredential(String providerCode) {
        return credentialMapper.selectListByProvider(
                        TenantContextHolder.getRequiredTenantId(), providerCode)
                .stream().anyMatch(candidate -> isUsable(candidate, providerCode));
    }

    public boolean isUsable(RentalLogisticsProviderCredentialDO credential, String providerCode) {
        return credential != null
                && Objects.equals(providerCode, credential.getProviderCode())
                && Boolean.TRUE.equals(credential.getEnabled())
                && StringUtils.hasText(credential.getCustomerCode())
                && StringUtils.hasText(credential.getApiKey());
    }
}
