package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDevicePerformanceReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalProductSkuReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportOverviewRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportQueryReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportSourceRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalRevenueReportRespVO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalReportMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalReportAdminServiceTest {

    private XianyuOrderMapper orderMapper;
    private RentalReportMapper reportMapper;
    private RentalReportAdminService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(XianyuOrderMapper.class);
        reportMapper = mock(RentalReportMapper.class);
        service = new RentalReportAdminService(orderMapper, reportMapper);
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void revenueSummaryShouldUseDatabaseAggregates() {
        when(orderMapper.selectRevenueSummary(2L)).thenReturn(Map.of(
                "order_count", 3L,
                "rent_amount_fen", 12_345L,
                "refund_amount_fen", 678L));

        RentalRevenueReportRespVO result = service.getRevenueSummary(2L);

        assertEquals(3, result.getOrderCount());
        assertEquals(12_345L, result.getRentAmountFen());
        assertEquals(678L, result.getRefundAmountFen());
        assertEquals("CNY", result.getCurrency());
    }

    @Test
    void revenueSummaryShouldReturnZeroForEmptyResult() {
        when(orderMapper.selectRevenueSummary(null)).thenReturn(null);

        RentalRevenueReportRespVO result = service.getRevenueSummary(null);

        assertEquals(0, result.getOrderCount());
        assertEquals(0L, result.getRentAmountFen());
        assertEquals(0L, result.getRefundAmountFen());
    }

    @Test
    void overviewShouldCalculateIdleDaysAndUtilization() {
        RentalReportQueryReqVO reqVO = query();
        RentalReportOverviewRespVO aggregate = new RentalReportOverviewRespVO();
        aggregate.setOrderCount(3);
        aggregate.setRentAmountFen(12_345L);
        aggregate.setRefundAmountFen(678L);
        aggregate.setDeviceCount(2);
        aggregate.setTotalDeviceDays(20L);
        aggregate.setOccupiedDeviceDays(7L);
        aggregate.setAssignedIncomeFen(8_000L);
        RentalReportSourceRespVO source = new RentalReportSourceRespVO();
        source.setSourceType("XIANYU");
        when(reportMapper.selectOverview(9L, reqVO.getStartDate(), LocalDate.of(2026, 7, 11)))
                .thenReturn(aggregate);
        when(reportMapper.selectSourceBreakdown(9L, reqVO.getStartDate(), LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(source));

        RentalReportOverviewRespVO result = service.getOverview(reqVO);

        assertSame(aggregate, result);
        assertEquals(13L, result.getIdleDeviceDays());
        assertEquals(3500, result.getUtilizationBasisPoints());
        assertEquals("CNY", result.getCurrency());
        assertEquals(List.of(source), result.getSources());
    }

    @Test
    void productSkuPageShouldUseDatabasePagination() {
        RentalReportQueryReqVO reqVO = query();
        RentalProductSkuReportRespVO row = new RentalProductSkuReportRespVO();
        Page<RentalProductSkuReportRespVO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(row));
        when(reportMapper.selectProductSkuPage(any(), eq(9L), eq(reqVO.getStartDate()),
                eq(LocalDate.of(2026, 7, 11)))).thenReturn(page);

        PageResult<RentalProductSkuReportRespVO> result = service.getProductSkuPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(List.of(row), result.getList());
    }

    @Test
    void devicePageShouldCalculatePerDeviceIdleAndUtilization() {
        RentalReportQueryReqVO reqVO = query();
        RentalDevicePerformanceReportRespVO row = new RentalDevicePerformanceReportRespVO();
        row.setTotalDays(10L);
        row.setOccupiedDays(4L);
        Page<RentalDevicePerformanceReportRespVO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(row));
        when(reportMapper.selectDevicePerformancePage(any(), eq(9L), eq(reqVO.getStartDate()),
                eq(LocalDate.of(2026, 7, 11)))).thenReturn(page);

        PageResult<RentalDevicePerformanceReportRespVO> result = service.getDevicePerformancePage(reqVO);

        assertEquals(6L, result.getList().get(0).getIdleDays());
        assertEquals(4000, result.getList().get(0).getUtilizationBasisPoints());
        verify(reportMapper).selectDevicePerformancePage(any(), eq(9L), eq(reqVO.getStartDate()),
                eq(LocalDate.of(2026, 7, 11)));
    }

    private RentalReportQueryReqVO query() {
        RentalReportQueryReqVO reqVO = new RentalReportQueryReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setStartDate(LocalDate.of(2026, 7, 1));
        reqVO.setEndDate(LocalDate.of(2026, 7, 10));
        return reqVO;
    }

}
