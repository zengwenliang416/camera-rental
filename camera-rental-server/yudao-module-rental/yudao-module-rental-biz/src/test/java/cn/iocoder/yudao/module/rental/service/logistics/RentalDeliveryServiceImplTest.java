package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsCarrierMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryDirectionEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeliveryServiceImplTest {

    private final RentalDeliveryMapper deliveryMapper = mock(RentalDeliveryMapper.class);
    private final RentalDeliveryDeviceRelMapper relationMapper = mock(RentalDeliveryDeviceRelMapper.class);
    private final RentalOrderMapper orderMapper = mock(RentalOrderMapper.class);
    private final RentalOrderItemMapper orderItemMapper = mock(RentalOrderItemMapper.class);
    private final RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
    private final RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
    private final RentalCarrierMappingService mappingService = mock(RentalCarrierMappingService.class);
    private final RentalLogisticsProviderConfigService configService =
            mock(RentalLogisticsProviderConfigService.class);
    private final RentalDeliveryOutboxService outboxService = mock(RentalDeliveryOutboxService.class);
    private final RentalDeliveryService service = new RentalDeliveryServiceImpl(deliveryMapper, relationMapper,
            orderMapper, orderItemMapper, assignmentMapper, deviceMapper, mappingService, configService,
            outboxService, new WaybillPrivacy());

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(9L);
        when(mappingService.resolve(any(), any())).thenAnswer(invocation -> {
            String sourceType = invocation.getArgument(0, String.class).trim().toUpperCase();
            String sourceCarrierCode = invocation.getArgument(1, String.class).trim().toUpperCase();
            return new RentalCarrierResolution(sourceType, sourceCarrierCode, sourceCarrierCode, null);
        });
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void createsLocalDeliveryWhenMappingIsMissingAndEnqueuesSafeTasks() {
        stubValidRelations(40L);
        when(deliveryMapper.selectMaxPackageSeq(9L, 30L, "OUTBOUND")).thenReturn(0);
        doAnswer(invocation -> {
            RentalDeliveryDO delivery = invocation.getArgument(0);
            delivery.setId(99L);
            return 1;
        }).when(deliveryMapper).insert(any(RentalDeliveryDO.class));
        when(outboxService.listPendingEventTypes(99L))
                .thenReturn(List.of("SUBSCRIBE", "INITIAL_QUERY"));

        RentalDeliveryResult result = service.createOrReuse(command(40L));

        assertTrue(result.created());
        assertEquals(99L, result.deliveryId());
        assertEquals("MAPPING_REQUIRED", result.mappingStatus());
        assertEquals("MAPPING_REQUIRED", result.subscribeStatus());
        assertEquals("MAPPING_REQUIRED", result.reasonCode());
        assertEquals(List.of("SUBSCRIBE", "INITIAL_QUERY"), result.pendingEventTypes());
        ArgumentCaptor<RentalDeliveryDO> deliveryCaptor = ArgumentCaptor.forClass(RentalDeliveryDO.class);
        verify(deliveryMapper).insert(deliveryCaptor.capture());
        assertEquals("SF1234567890", deliveryCaptor.getValue().getNormalizedWaybillNo());
        verify(relationMapper).insert(any(RentalDeliveryDeviceRelDO.class));
        verify(outboxService).enqueue(99L, RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE, null,
                "delivery tracking subscription");
        verify(outboxService).enqueue(99L, RentalDeliveryOutboxEventTypeEnum.INITIAL_QUERY, null,
                "delivery initial tracking query");
    }

    @Test
    void reusesExistingDeliveryAndDoesNotDuplicateInitialTasks() {
        stubValidRelations(40L);
        RentalDeliveryDO existing = RentalDeliveryDO.builder()
                .id(99L)
                .rentalOrderId(30L)
                .mappingStatus("READY")
                .subscribeStatus("SUBSCRIBED")
                .queryStatus("READY")
                .waybillNo("SF1234567890")
                .build();
        when(deliveryMapper.selectByBusinessKeyForUpdate(9L, 30L, "OUTBOUND", "SF",
                "SF1234567890")).thenReturn(existing);
        when(outboxService.listPendingEventTypes(99L)).thenReturn(List.of());

        RentalDeliveryResult result = service.createOrReuse(command(40L));

        assertFalse(result.created());
        verify(deliveryMapper, never()).insert(any(RentalDeliveryDO.class));
        verify(outboxService, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void readsCurrentDeliveryStateForIdempotentShipmentReplay() {
        RentalDeliveryDO existing = RentalDeliveryDO.builder()
                .id(99L)
                .mappingStatus("READY")
                .subscribeStatus("SUBSCRIBED")
                .queryStatus("RETRY_WAIT")
                .waybillNo("SF1234567890")
                .lastErrorCode("PROVIDER_TIMEOUT")
                .build();
        when(deliveryMapper.selectByTenantIdAndId(9L, 99L)).thenReturn(existing);
        when(outboxService.listPendingEventTypes(99L)).thenReturn(List.of("INITIAL_QUERY"));

        RentalDeliveryResult result = service.getResult(99L);

        assertFalse(result.created());
        assertEquals("PROVIDER_TIMEOUT", result.reasonCode());
        assertEquals(List.of("INITIAL_QUERY"), result.pendingEventTypes());
        assertEquals("SF1****7890", result.maskedWaybillNo());
    }

    @Test
    void rejectsAssignmentThatPointsToAnotherDevice() {
        stubValidRelations(41L);
        when(deliveryMapper.selectMaxPackageSeq(9L, 30L, "OUTBOUND")).thenReturn(0);
        doAnswer(invocation -> {
            RentalDeliveryDO delivery = invocation.getArgument(0);
            delivery.setId(99L);
            return 1;
        }).when(deliveryMapper).insert(any(RentalDeliveryDO.class));

        RentalLogisticsException exception =
                assertThrows(RentalLogisticsException.class, () -> service.createOrReuse(command(40L)));

        assertEquals("DELIVERY_DEVICE_REL_MISMATCH", exception.getCode());
        verify(relationMapper, never()).insert(any(RentalDeliveryDeviceRelDO.class));
        verify(outboxService, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void validatesEveryDuplicateDeviceRelationBeforeInserting() {
        stubValidRelations(40L);
        RentalDeviceAssignmentDO incompatibleAssignment = RentalDeviceAssignmentDO.builder()
                .id(61L)
                .rentalOrderId(30L)
                .rentalOrderItemId(50L)
                .deviceId(41L)
                .build();
        incompatibleAssignment.setTenantId(9L);
        when(assignmentMapper.selectByIdForUpdate(61L)).thenReturn(incompatibleAssignment);
        when(deliveryMapper.selectMaxPackageSeq(9L, 30L, "OUTBOUND")).thenReturn(0);
        doAnswer(invocation -> {
            RentalDeliveryDO delivery = invocation.getArgument(0);
            delivery.setId(99L);
            return 1;
        }).when(deliveryMapper).insert(any(RentalDeliveryDO.class));
        RentalDeliveryCreateCommand duplicateDeviceCommand = new RentalDeliveryCreateCommand(
                30L, RentalDeliveryDirectionEnum.OUTBOUND, "XIANYU", "shipment-1", "SF",
                "Shunfeng", "sf-123 456 7890", "19900000000",
                List.of(
                        new RentalDeliveryDeviceCommand(50L, 60L, 40L),
                        new RentalDeliveryDeviceCommand(50L, 61L, 40L)));

        RentalLogisticsException exception = assertThrows(RentalLogisticsException.class,
                () -> service.createOrReuse(duplicateDeviceCommand));

        assertEquals("DELIVERY_DEVICE_REL_MISMATCH", exception.getCode());
        verify(relationMapper, never()).insert(any(RentalDeliveryDeviceRelDO.class));
        verify(outboxService, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void marksPhoneRequiredAndDoesNotEnqueueProviderTasks() {
        stubValidRelations(40L);
        RentalLogisticsCarrierMappingDO mapping = RentalLogisticsCarrierMappingDO.builder()
                .canonicalCarrierCode("SF")
                .providerCode("KUAIDI100")
                .providerCarrierCode("shunfeng")
                .phoneRequirement("REQUIRED")
                .build();
        when(mappingService.resolve("XIANYU", "SF"))
                .thenReturn(new RentalCarrierResolution("XIANYU", "SF", "SF", mapping));
        when(configService.get("KUAIDI100")).thenReturn(RentalLogisticsProviderConfigDO.builder()
                .enabled(true)
                .queryEnabled(true)
                .subscribeEnabled(true)
                .build());
        when(deliveryMapper.selectMaxPackageSeq(9L, 30L, "OUTBOUND")).thenReturn(0);
        doAnswer(invocation -> {
            RentalDeliveryDO delivery = invocation.getArgument(0);
            delivery.setId(99L);
            return 1;
        }).when(deliveryMapper).insert(any(RentalDeliveryDO.class));
        RentalDeliveryCreateCommand missingPhoneCommand = new RentalDeliveryCreateCommand(
                30L, RentalDeliveryDirectionEnum.OUTBOUND, "XIANYU", "shipment-1", "SF",
                "Shunfeng", "sf-123 456 7890", null,
                List.of(new RentalDeliveryDeviceCommand(50L, 60L, 40L)));

        RentalDeliveryResult result = service.createOrReuse(missingPhoneCommand);

        assertEquals("PHONE_REQUIRED", result.subscribeStatus());
        assertEquals("PHONE_REQUIRED", result.queryStatus());
        assertEquals("TRACKING_PHONE_REQUIRED", result.reasonCode());
        verify(outboxService, never()).enqueue(any(), any(), any(), any());
    }

    private void stubValidRelations(Long assignmentDeviceId) {
        RentalOrderDO order = RentalOrderDO.builder().id(30L).build();
        order.setTenantId(9L);
        RentalOrderItemDO item = RentalOrderItemDO.builder().id(50L).rentalOrderId(30L).build();
        item.setTenantId(9L);
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(60L)
                .rentalOrderId(30L)
                .rentalOrderItemId(50L)
                .deviceId(assignmentDeviceId)
                .build();
        assignment.setTenantId(9L);
        RentalDeviceDO device = RentalDeviceDO.builder().id(40L).build();
        device.setTenantId(9L);
        when(orderMapper.selectByIdForUpdate(30L)).thenReturn(order);
        when(orderItemMapper.selectByIdForUpdate(50L)).thenReturn(item);
        when(assignmentMapper.selectByIdForUpdate(60L)).thenReturn(assignment);
        when(deviceMapper.selectByIdForUpdate(40L)).thenReturn(device);
    }

    private RentalDeliveryCreateCommand command(Long deviceId) {
        return new RentalDeliveryCreateCommand(30L, RentalDeliveryDirectionEnum.OUTBOUND, "XIANYU",
                "shipment-1", "SF", "Shunfeng", "sf-123 456 7890", "19900000000",
                List.of(new RentalDeliveryDeviceCommand(50L, 60L, deviceId)));
    }
}
