package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.CatalogModel;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.DeviceNumberReservation;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceQrCodec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(catalogService.reserveDeviceNumbers(" dji ", " p4p ", 1))
                .thenReturn(new DeviceNumberReservation(
                        new CatalogModel(1L, "DJI", "大疆", 2L, "P4P", "P4P", "P4P"),
                        List.of("P4P-01")));
        when(deviceMapper.insert(any(RentalDeviceDO.class))).thenAnswer(invocation -> {
            RentalDeviceDO device = invocation.getArgument(0);
            device.setId(101L);
            return 1;
        });

        RentalDeviceCreateReqVO reqVO = createRequest(" dji ", " p4p ");
        assertEquals(101L, service.createDevice(reqVO));

        ArgumentCaptor<RentalDeviceDO> captor = ArgumentCaptor.forClass(RentalDeviceDO.class);
        verify(deviceMapper).insert(captor.capture());
        verify(catalogService).reserveDeviceNumbers(" dji ", " p4p ", 1);
        assertEquals("DJI", captor.getValue().getCategoryCode());
        assertEquals("P4P", captor.getValue().getEquipmentModelCode());
        assertEquals("P4P-01", captor.getValue().getDeviceNo());
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

    private static RentalDeviceCreateReqVO createRequest(String categoryCode, String modelCode) {
        RentalDeviceCreateReqVO reqVO = new RentalDeviceCreateReqVO();
        reqVO.setCategoryCode(categoryCode);
        reqVO.setEquipmentModelCode(modelCode);
        return reqVO;
    }

}
