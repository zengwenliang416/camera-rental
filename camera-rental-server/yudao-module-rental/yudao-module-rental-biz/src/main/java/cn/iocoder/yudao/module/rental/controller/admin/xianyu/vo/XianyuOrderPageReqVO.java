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
    private String conversionStatus;
    private String externalProductId;
    private String externalSkuId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @AssertTrue(message = "订单统计结束日不能早于开始日")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

}
