package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - 闲鱼渠道订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class XianyuOrderPageReqVO extends PageParam {

    private Long shopId;
    private String orderStatus;
    private String conversionStatus;
    /** Exact or partial match for ops lookup (full value, not redacted). */
    private String externalOrderId;
    private String xgjProductId;
    private String xianyuItemId;
    private String xgjSkuId;
    private String xianyuSkuId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate shipDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate rentalStartDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate rentalEndDate;

    @AssertTrue(message = "订单统计结束日不能早于开始日")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "租期开始日和结束日必须同时填写")
    public boolean isRentalDateRangeComplete() {
        return (rentalStartDate == null) == (rentalEndDate == null);
    }

    @AssertTrue(message = "租期结束日不能早于开始日")
    public boolean isRentalDateRangeValid() {
        return rentalStartDate == null || rentalEndDate == null || !rentalEndDate.isBefore(rentalStartDate);
    }

}
