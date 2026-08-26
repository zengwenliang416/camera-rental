package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    private static RentalDeviceAdminService createService(RentalDeviceMapper deviceMapper,
                                                          RentalDeviceCatalogService catalogService) {
        RentalDeviceProperties properties = new RentalDeviceProperties();
        return new RentalDeviceAdminService(
                deviceMapper,
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

}
