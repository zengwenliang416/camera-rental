package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RentalLogisticsBackfillTransactionServiceTest {

    private final RentalLogisticsOperationsMapper operationsMapper = mock(RentalLogisticsOperationsMapper.class);
    private final RentalDeliveryService deliveryService = mock(RentalDeliveryService.class);
    private final RentalLogisticsBackfillTransactionService service =
            new RentalLogisticsBackfillTransactionService(operationsMapper, deliveryService);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void createAndShipmentBindingShareOneRollbackTransaction() throws Exception {
        Transactional transactional = RentalLogisticsBackfillTransactionService.class
                .getMethod("apply", RentalLogisticsOperationsMapper.BackfillCandidateRow.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertArrayEquals(new Class<?>[]{Exception.class}, transactional.rollbackFor());
    }

    @Test
    void bindingFailureThrowsSoNewDeliveryCannotCommitAsOrphan() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        when(deliveryService.createOrReuseLocalOnly(any()))
                .thenReturn(new RentalDeliveryResult(99L, true, "READY", "PENDING",
                        "PENDING", "SF5****2626", null, List.of()));
        when(operationsMapper.bindShipmentDelivery(9L, 10L, 99L)).thenReturn(0);

        RentalLogisticsException exception = assertThrows(RentalLogisticsException.class,
                () -> service.apply(candidate));

        assertEquals("BACKFILL_SHIPMENT_BIND_CONFLICT", exception.getCode());
        verify(deliveryService).createOrReuseLocalOnly(any());
        verify(operationsMapper).bindShipmentDelivery(9L, 10L, 99L);
    }

    @Test
    void localOnlyCreationNeverUsesProviderFacingServiceMethod() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow candidate = candidate();
        when(deliveryService.createOrReuseLocalOnly(any()))
                .thenReturn(new RentalDeliveryResult(99L, true, "READY", "PENDING",
                        "PENDING", "SF5****2626", null, List.of()));
        when(operationsMapper.bindShipmentDelivery(9L, 10L, 99L)).thenReturn(1);

        service.apply(candidate);

        verify(deliveryService).createOrReuseLocalOnly(any());
        verify(deliveryService, never()).createOrReuse(any());
    }

    private RentalLogisticsOperationsMapper.BackfillCandidateRow candidate() {
        RentalLogisticsOperationsMapper.BackfillCandidateRow row =
                new RentalLogisticsOperationsMapper.BackfillCandidateRow();
        row.setShipmentId(10L);
        row.setAssignmentId(30L);
        row.setDeviceId(40L);
        row.setRentalOrderId(500L);
        row.setRentalOrderItemId(600L);
        row.setReceiverMobile("13800138000");
        row.setWaybillNo("SF5159187992626");
        row.setExpressCode("SF");
        row.setExpressName("顺丰速运");
        return row;
    }
}
