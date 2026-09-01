package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Service
public class RentalChannelOrderReconciliationTrigger {

    private final ApplicationEventPublisher eventPublisher;
    private final RentalChannelReconciliationRunService runService;

    public RentalChannelOrderReconciliationTrigger(
            ApplicationEventPublisher eventPublisher,
            RentalChannelReconciliationRunService runService) {
        this.eventPublisher = eventPublisher;
        this.runService = runService;
    }

    public Long afterRuleChange(Long productRuleId, Long shopId, String xianyuItemId) {
        if (shopId == null || !StringUtils.hasText(xianyuItemId)) {
            return null;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long runId = runService.createRuleChangeRun(
                productRuleId, shopId, xianyuItemId.trim());
        eventPublisher.publishEvent(RentalChannelOrderReconciliationRequestedEvent.forTrackedItem(
                tenantId, shopId, xianyuItemId.trim(), runId));
        return runId;
    }

    public void afterProductChange(Long shopId, String xgjProductId) {
        if (shopId == null || !StringUtils.hasText(xgjProductId)) {
            return;
        }
        eventPublisher.publishEvent(RentalChannelOrderReconciliationRequestedEvent.forProduct(
                TenantContextHolder.getRequiredTenantId(), shopId, xgjProductId.trim()));
    }

    public void afterSkuChange(Long shopId, String xgjProductId, Collection<String> xgjSkuIds) {
        List<String> normalizedSkuIds = xgjSkuIds == null ? List.of() : xgjSkuIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (shopId == null || !StringUtils.hasText(xgjProductId) || normalizedSkuIds.isEmpty()) {
            return;
        }
        eventPublisher.publishEvent(RentalChannelOrderReconciliationRequestedEvent.forProductSkus(
                TenantContextHolder.getRequiredTenantId(), shopId,
                xgjProductId.trim(), normalizedSkuIds));
    }

}
