package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceUpdateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.CatalogModel;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.DeviceNumberSelection;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceQrCodec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NO_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DISABLE_BLOCKED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_SERIAL_DUPLICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class RentalDeviceAdminServiceTest {

    @Test
    void createsDeviceWithNormalizedCategoryAndModel() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceCatalogService catalogService = mock(RentalDeviceCatalogService.class);
        RentalDeviceAdminService service = createService(deviceMapper, catalogService);
        when(catalogService.composeDeviceNumber(" dji ", " p4p ", "2"))
                .thenReturn(new DeviceNumberSelection(
                        new CatalogModel(1L, "DJI", "大疆", 2L, "P4P", "P4P", "P4P"),
                        "P4P-02"));
        when(deviceMapper.insert(any(RentalDeviceDO.class))).thenAnswer(invocation -> {
            RentalDeviceDO device = invocation.getArgument(0);
            device.setId(101L);
            return 1;
        });

        RentalDeviceCreateReqVO reqVO = createRequest(" dji ", " p4p ", "2");
        assertEquals(101L, service.createDevice(reqVO));

        ArgumentCaptor<RentalDeviceDO> captor = ArgumentCaptor.forClass(RentalDeviceDO.class);
        verify(deviceMapper).insert(captor.capture());
        verify(catalogService).composeDeviceNumber(" dji ", " p4p ", "2");
        assertEquals("DJI", captor.getValue().getCategoryCode());
        assertEquals("P4P", captor.getValue().getEquipmentModelCode());
        assertEquals("P4P-02", captor.getValue().getDeviceNo());
    }

    @Test
    void rejectsExistingAdministratorSelectedDeviceNumber() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceCatalogService catalogService = mock(RentalDeviceCatalogService.class);
        RentalDeviceAdminService service = createService(deviceMapper, catalogService);
        when(catalogService.composeDeviceNumber("DJI", "P4P", "02"))
                .thenReturn(new DeviceNumberSelection(
                        new CatalogModel(1L, "DJI", "大疆", 2L, "P4P", "P4P", "P4P"),
                        "P4P-02"));
        when(deviceMapper.selectByDeviceNo("P4P-02"))
                .thenReturn(RentalDeviceDO.builder().id(9L).deviceNo("P4P-02").build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createDevice(createRequest("DJI", "P4P", "02")));

        assertEquals(RENTAL_DEVICE_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void classifiesConcurrentDeviceNumberCollision() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceCatalogService catalogService = mock(RentalDeviceCatalogService.class);
        RentalDeviceAdminService service = createService(deviceMapper, catalogService);
        when(catalogService.composeDeviceNumber("DJI", "P4P", "02"))
                .thenReturn(new DeviceNumberSelection(
                        new CatalogModel(1L, "DJI", "大疆", 2L, "P4P", "P4P", "P4P"),
                        "P4P-02"));
        when(deviceMapper.selectByDeviceNo("P4P-02"))
                .thenReturn(null)
                .thenReturn(RentalDeviceDO.builder().id(9L).deviceNo("P4P-02").build());
        when(deviceMapper.insert(any(RentalDeviceDO.class)))
                .thenThrow(new DuplicateKeyException("device number collision"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createDevice(createRequest("DJI", "P4P", "02")));

        assertEquals(RENTAL_DEVICE_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void preservesUnrelatedUniqueKeyCollision() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceCatalogService catalogService = mock(RentalDeviceCatalogService.class);
        RentalDeviceAdminService service = createService(deviceMapper, catalogService);
        when(catalogService.composeDeviceNumber("DJI", "P4P", "03"))
                .thenReturn(new DeviceNumberSelection(
                        new CatalogModel(1L, "DJI", "大疆", 2L, "P4P", "P4P", "P4P"),
                        "P4P-03"));
        DuplicateKeyException collision = new DuplicateKeyException("serial number collision");
        when(deviceMapper.insert(any(RentalDeviceDO.class))).thenThrow(collision);

        DuplicateKeyException ex = assertThrows(DuplicateKeyException.class,
                () -> service.createDevice(createRequest("DJI", "P4P", "03")));

        assertEquals(collision, ex);
    }

    @Test
    void updatesOnlyMutableFieldsAndNormalizesEmptyValues() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        RentalDeviceAdminService service = createService(deviceMapper, assignmentMapper,
                mock(RentalDeviceDeletionGuard.class), mock(RentalDeviceCatalogService.class));
        when(deviceMapper.selectByIdForUpdate(8L)).thenReturn(RentalDeviceDO.builder()
                .id(8L).status("AVAILABLE").enabled(true).build());

        RentalDeviceUpdateReqVO reqVO = updateRequest(8L, "  SN-8  ", "   ", 1200, true);
        service.updateDevice(reqVO);

        verify(deviceMapper).updateMutableFields(8L, "SN-8", null, 1200, true);
    }

    @Test
    void rejectsDuplicateSerialNumber() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAdminService service = createService(deviceMapper,
                mock(RentalDeviceAssignmentMapper.class), mock(RentalDeviceDeletionGuard.class),
                mock(RentalDeviceCatalogService.class));
        RentalDeviceDO device = RentalDeviceDO.builder().id(8L).status("AVAILABLE").build();
        device.setTenantId(1L);
        when(deviceMapper.selectByIdForUpdate(8L)).thenReturn(device);
        when(deviceMapper.countAllBySerialNumberExcludingId(1L, "SN-8", 8L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateDevice(updateRequest(8L, "SN-8", null, null, true)));

        assertEquals(RENTAL_DEVICE_SERIAL_DUPLICATE.getCode(), ex.getCode());
        verify(deviceMapper, never()).updateMutableFields(any(), any(), any(), any(), any());
    }

    @Test
    void classifiesConcurrentSerialNumberCollision() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAdminService service = createService(deviceMapper,
                mock(RentalDeviceAssignmentMapper.class), mock(RentalDeviceDeletionGuard.class),
                mock(RentalDeviceCatalogService.class));
        RentalDeviceDO device = RentalDeviceDO.builder().id(8L).status("AVAILABLE").build();
        device.setTenantId(1L);
        when(deviceMapper.selectByIdForUpdate(8L)).thenReturn(device);
        when(deviceMapper.countAllBySerialNumberExcludingId(1L, "SN-8", 8L))
                .thenReturn(0L)
                .thenReturn(1L);
        when(deviceMapper.updateMutableFields(8L, "SN-8", null, null, true))
                .thenThrow(new DuplicateKeyException("serial number collision"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateDevice(updateRequest(8L, "SN-8", null, null, true)));

        assertEquals(RENTAL_DEVICE_SERIAL_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void rejectsDisablingNonAvailableDevice() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAdminService service = createService(deviceMapper,
                mock(RentalDeviceAssignmentMapper.class), mock(RentalDeviceDeletionGuard.class),
                mock(RentalDeviceCatalogService.class));
        when(deviceMapper.selectByIdForUpdate(8L)).thenReturn(RentalDeviceDO.builder()
                .id(8L).status("RENTED").build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateDevice(updateRequest(8L, null, null, null, false)));

        assertEquals(RENTAL_DEVICE_DISABLE_BLOCKED.getCode(), ex.getCode());
    }

    @Test
    void rejectsDisablingDeviceWithActiveAssignment() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        RentalDeviceAdminService service = createService(deviceMapper, assignmentMapper,
                mock(RentalDeviceDeletionGuard.class), mock(RentalDeviceCatalogService.class));
        when(deviceMapper.selectByIdForUpdate(8L)).thenReturn(RentalDeviceDO.builder()
                .id(8L).status("AVAILABLE").build());
        when(assignmentMapper.selectActiveByDeviceIdForUpdate(8L))
                .thenReturn(RentalDeviceAssignmentDO.builder().id(18L).deviceId(8L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateDevice(updateRequest(8L, null, null, null, false)));

        assertEquals(RENTAL_DEVICE_DISABLE_BLOCKED.getCode(), ex.getCode());
    }

    @Test
    void deletesOnlyAfterLockedGuardValidation() {
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceDeletionGuard deletionGuard = mock(RentalDeviceDeletionGuard.class);
        RentalDeviceAdminService service = createService(deviceMapper,
                mock(RentalDeviceAssignmentMapper.class), deletionGuard,
                mock(RentalDeviceCatalogService.class));
        RentalDeviceDO device = RentalDeviceDO.builder().id(8L).status("AVAILABLE").build();
        when(deviceMapper.selectByIdForUpdate(8L)).thenReturn(device);

        service.deleteDevice(8L);

        verify(deletionGuard).validateDeletable(device);
        verify(deviceMapper).deleteById(8L);
    }

    private static RentalDeviceAdminService createService(RentalDeviceMapper deviceMapper,
                                                          RentalDeviceCatalogService catalogService) {
        return createService(deviceMapper, mock(RentalDeviceAssignmentMapper.class),
                mock(RentalDeviceDeletionGuard.class), catalogService);
    }

    private static RentalDeviceAdminService createService(RentalDeviceMapper deviceMapper,
                                                          RentalDeviceAssignmentMapper assignmentMapper,
                                                          RentalDeviceDeletionGuard deletionGuard,
                                                          RentalDeviceCatalogService catalogService) {
        RentalDeviceProperties properties = new RentalDeviceProperties();
        return new RentalDeviceAdminService(
                deviceMapper,
                assignmentMapper,
                deletionGuard,
                mock(RentalDeviceAssignmentService.class),
                new RentalDeviceQrCodec(properties),
                properties,
                catalogService);
    }

    private static RentalDeviceCreateReqVO createRequest(String categoryCode, String modelCode,
                                                         String deviceNoSuffix) {
        RentalDeviceCreateReqVO reqVO = new RentalDeviceCreateReqVO();
        reqVO.setCategoryCode(categoryCode);
        reqVO.setEquipmentModelCode(modelCode);
        reqVO.setDeviceNoSuffix(deviceNoSuffix);
        return reqVO;
    }

    private static RentalDeviceUpdateReqVO updateRequest(Long id, String serialNumber,
                                                         String warehouseCode, Integer purchaseAmount,
                                                         Boolean enabled) {
        RentalDeviceUpdateReqVO reqVO = new RentalDeviceUpdateReqVO();
        reqVO.setId(id);
        reqVO.setSerialNumber(serialNumber);
        reqVO.setWarehouseCode(warehouseCode);
        reqVO.setPurchaseAmount(purchaseAmount);
        reqVO.setEnabled(enabled);
        return reqVO;
    }

}
