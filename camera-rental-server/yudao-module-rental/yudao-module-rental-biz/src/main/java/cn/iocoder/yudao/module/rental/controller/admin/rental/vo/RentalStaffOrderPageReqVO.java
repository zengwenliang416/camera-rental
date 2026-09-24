package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "员工订单查询，搜索范围包括订单号、商品、收件人姓名、电话和地址")
public class RentalStaffOrderPageReqVO extends PageParam {
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate orderDateStart;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate orderDateEnd;
    @jakarta.validation.constraints.AssertTrue(message = "请选择有效的下单日期范围")
    public boolean isOrderDateRangeValid() {
        if (orderDateStart == null && orderDateEnd == null) return true;
        return orderDateStart != null && orderDateEnd != null && !orderDateEnd.isBefore(orderDateStart)
                && orderDateStart.getYear() >= 2000 && orderDateEnd.getYear() <= 2100;
    }
    @Size(max = 100)
    private String keyword;
    @Pattern(regexp = "ALL|PENDING_ALLOCATION|UNSHIPPED|PARTIAL|SHIPPED")
    private String queue = "ALL";
}
