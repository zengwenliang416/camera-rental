package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDevicePerformanceReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalProductSkuReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportOverviewRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportQueryReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalRevenueReportRespVO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalReportMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Source-linked revenue report: rent uses pay_amount and refunds remain a separate metric.
 */
@Service
public class RentalReportAdminService {

    private final XianyuOrderMapper orderMapper;
    private final RentalReportMapper reportMapper;

    public RentalReportAdminService(XianyuOrderMapper orderMapper, RentalReportMapper reportMapper) {
        this.orderMapper = orderMapper;
        this.reportMapper = reportMapper;
    }

    public RentalRevenueReportRespVO getRevenueSummary(Long shopId) {
        Map<String, Object> summary = orderMapper.selectRevenueSummary(shopId);
        summary = summary == null ? Map.of() : summary;
        RentalRevenueReportRespVO vo = new RentalRevenueReportRespVO();
        vo.setOrderCount(Math.toIntExact(getLong(summary, "order_count")));
        vo.setRentAmountFen(getLong(summary, "rent_amount_fen"));
        vo.setRefundAmountFen(getLong(summary, "refund_amount_fen"));
        vo.setCurrency("CNY");
        return vo;
    }

    public RentalReportOverviewRespVO getOverview(RentalReportQueryReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDate endDateExclusive = reqVO.getEndDate().plusDays(1);
        RentalReportOverviewRespVO result = reportMapper.selectOverview(
                tenantId, reqVO.getStartDate(), endDateExclusive);
        if (result == null) {
            result = new RentalReportOverviewRespVO();
        }
        result.setStartDate(reqVO.getStartDate());
        result.setEndDate(reqVO.getEndDate());
        result.setOrderCount(defaultValue(result.getOrderCount()));
        result.setRentAmountFen(defaultValue(result.getRentAmountFen()));
        result.setRefundAmountFen(defaultValue(result.getRefundAmountFen()));
        result.setDeviceCount(defaultValue(result.getDeviceCount()));
        result.setTotalDeviceDays(defaultValue(result.getTotalDeviceDays()));
        result.setOccupiedDeviceDays(defaultValue(result.getOccupiedDeviceDays()));
        result.setIdleDeviceDays(Math.max(0L, result.getTotalDeviceDays() - result.getOccupiedDeviceDays()));
        result.setUtilizationBasisPoints(calculateBasisPoints(
                result.getOccupiedDeviceDays(), result.getTotalDeviceDays()));
        result.setAssignedIncomeFen(defaultValue(result.getAssignedIncomeFen()));
        result.setCurrency("CNY");
        result.setSources(reportMapper.selectSourceBreakdown(
                tenantId, reqVO.getStartDate(), endDateExclusive));
        return result;
    }

    public PageResult<RentalProductSkuReportRespVO> getProductSkuPage(RentalReportQueryReqVO reqVO) {
        IPage<RentalProductSkuReportRespVO> page = reportMapper.selectProductSkuPage(
                new Page<>(reqVO.getPageNo(), reqVO.getPageSize()),
                TenantContextHolder.getRequiredTenantId(),
                reqVO.getStartDate(),
                reqVO.getEndDate().plusDays(1));
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    public PageResult<RentalDevicePerformanceReportRespVO> getDevicePerformancePage(
            RentalReportQueryReqVO reqVO) {
        IPage<RentalDevicePerformanceReportRespVO> page = reportMapper.selectDevicePerformancePage(
                new Page<>(reqVO.getPageNo(), reqVO.getPageSize()),
                TenantContextHolder.getRequiredTenantId(),
                reqVO.getStartDate(),
                reqVO.getEndDate().plusDays(1));
        page.getRecords().forEach(row -> {
            row.setTotalDays(defaultValue(row.getTotalDays()));
            row.setOccupiedDays(defaultValue(row.getOccupiedDays()));
            row.setIdleDays(Math.max(0L, row.getTotalDays() - row.getOccupiedDays()));
            row.setUtilizationBasisPoints(calculateBasisPoints(row.getOccupiedDays(), row.getTotalDays()));
            row.setScheduleCount(defaultValue(row.getScheduleCount()));
            row.setAssignmentCount(defaultValue(row.getAssignmentCount()));
            row.setAssignedIncomeFen(defaultValue(row.getAssignedIncomeFen()));
        });
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    private static long getLong(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static int calculateBasisPoints(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0;
        }
        return Math.toIntExact(Math.min(10_000L, Math.round(numerator * 10_000D / denominator)));
    }

    private static long defaultValue(Long value) {
        return value == null ? 0L : value;
    }

    private static int defaultValue(Integer value) {
        return value == null ? 0 : value;
    }

}
