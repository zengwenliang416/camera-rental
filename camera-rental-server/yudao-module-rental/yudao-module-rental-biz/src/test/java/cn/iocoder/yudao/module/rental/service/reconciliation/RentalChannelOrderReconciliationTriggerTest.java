package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RentalChannelOrderReconciliationTriggerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RentalChannelReconciliationRunService runService;

    private RentalChannelOrderReconciliationTrigger trigger;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        trigger = new RentalChannelOrderReconciliationTrigger(eventPublisher, runService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void ruleChangePublishesTenantScopedExactItemEventWithoutQueryingOrdersInline() {
        org.mockito.Mockito.when(runService.createRuleChangeRun(40L, 7L, "item-1"))
                .thenReturn(80L);
        assertEquals(80L, trigger.afterRuleChange(40L, 7L, " item-1 "));

        RentalChannelOrderReconciliationRequestedEvent event = captureEvent();
        assertEquals(9L, event.tenantId());
        assertEquals(RentalChannelOrderReconciliationRequestedEvent.Scope.ITEM, event.scope());
        assertEquals(7L, event.shopId());
        assertEquals("item-1", event.xianyuItemId());
        assertNull(event.xgjProductId());
        assertEquals(80L, event.reconciliationRunId());
    }

    @Test
    void skuChangeNormalizesExactProductAndSkuOwnershipIntoOneEvent() {
        trigger.afterSkuChange(
                7L, " product-1 ", List.of(" sku-1 ", "sku-2", "sku-1"));

        RentalChannelOrderReconciliationRequestedEvent event = captureEvent();
        assertEquals(RentalChannelOrderReconciliationRequestedEvent.Scope.PRODUCT_SKUS, event.scope());
        assertEquals("product-1", event.xgjProductId());
        assertEquals(List.of("sku-1", "sku-2"), event.xgjSkuIds());
    }

    @Test
    void productChangePublishesExactShopAndProductEvent() {
        trigger.afterProductChange(7L, " product-1 ");

        RentalChannelOrderReconciliationRequestedEvent event = captureEvent();
        assertEquals(RentalChannelOrderReconciliationRequestedEvent.Scope.PRODUCT, event.scope());
        assertEquals(7L, event.shopId());
        assertEquals("product-1", event.xgjProductId());
    }

    @Test
    void incompleteIdentifiersDoNotPublishEvents() {
        assertNull(trigger.afterRuleChange(40L, null, "item-1"));
        trigger.afterProductChange(7L, " ");
        trigger.afterSkuChange(7L, "product-1", List.of());

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(runService);
    }

    private RentalChannelOrderReconciliationRequestedEvent captureEvent() {
        ArgumentCaptor<RentalChannelOrderReconciliationRequestedEvent> captor =
                ArgumentCaptor.forClass(RentalChannelOrderReconciliationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

}
