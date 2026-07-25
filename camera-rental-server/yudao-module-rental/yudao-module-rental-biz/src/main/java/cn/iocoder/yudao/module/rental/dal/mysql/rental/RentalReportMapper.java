package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDevicePerformanceReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalProductSkuReportRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportOverviewRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalReportSourceRespVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RentalReportMapper {

    RentalReportOverviewRespVO selectOverview(@Param("tenantId") Long tenantId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDateExclusive") LocalDate endDateExclusive);

    List<RentalReportSourceRespVO> selectSourceBreakdown(@Param("tenantId") Long tenantId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDateExclusive") LocalDate endDateExclusive);

    IPage<RentalProductSkuReportRespVO> selectProductSkuPage(
            IPage<RentalProductSkuReportRespVO> page,
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive);

    IPage<RentalDevicePerformanceReportRespVO> selectDevicePerformancePage(
            IPage<RentalDevicePerformanceReportRespVO> page,
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive);

}
