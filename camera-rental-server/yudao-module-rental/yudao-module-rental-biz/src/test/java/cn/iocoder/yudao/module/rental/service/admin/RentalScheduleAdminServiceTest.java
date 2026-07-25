package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalSchedulePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RentalScheduleAdminServiceTest {

    private RentalScheduleMapper scheduleMapper;
    private RentalDeviceMapper deviceMapper;
    private RentalOrderItemMapper orderItemMapper;
    private RentalScheduleAdminService service;

    @BeforeEach
    void setUp() {
        scheduleMapper = mock(RentalScheduleMapper.class);
        deviceMapper = mock(RentalDeviceMapper.class);
        orderItemMapper = mock(RentalOrderItemMapper.class);
        service = new RentalScheduleAdminService(scheduleMapper, deviceMapper, orderItemMapper);
    }

    @Test
    void getSchedulePageShouldExposeBillableAndOccupiedRanges() {
        RentalScheduleDO schedule = RentalScheduleDO.builder()
                .id(1L)
                .deviceId(2L)
                .rentalOrderId(3L)
                .rentalOrderItemId(4L)
                .scheduleType("RENTAL")
                .status("EFFECTIVE")
                .occupyStartDate(LocalDate.of(2026, 7, 22))
                .occupyEndDateExclusive(LocalDate.of(2026, 7, 31))
                .build();
        when(scheduleMapper.selectPage(any(RentalSchedulePageReqVO.class), any()))
                .thenReturn(new PageResult<>(List.of(schedule), 1L));
        when(deviceMapper.selectByIds(Set.of(2L))).thenReturn(List.of(RentalDeviceDO.builder()
                .id(2L).deviceNo("A7M4-0001").equipmentModelCode("SONY-A7M4").build()));
        when(orderItemMapper.selectByIds(Set.of(4L))).thenReturn(List.of(RentalOrderItemDO.builder()
                .id(4L)
                .billableStartDate(LocalDate.of(2026, 7, 25))
                .billableEndDate(LocalDate.of(2026, 7, 27))
                .build()));

        PageResult<RentalScheduleRespVO> result = service.getSchedulePage(new RentalSchedulePageReqVO());

        RentalScheduleRespVO row = result.getList().get(0);
        assertEquals("A7M4-0001", row.getDeviceNo());
        assertEquals(LocalDate.of(2026, 7, 25), row.getBillableStartDate());
        assertEquals(LocalDate.of(2026, 7, 27), row.getBillableEndDate());
        assertEquals(LocalDate.of(2026, 7, 22), row.getOccupyStartDate());
        assertEquals(LocalDate.of(2026, 7, 31), row.getOccupyEndDateExclusive());
    }

}
