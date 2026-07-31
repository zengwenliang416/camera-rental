package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 批量查询本地物流摘要请求")
@Data
public class RentalDeliveryTrackingBatchReqVO {

    @NotEmpty
    @Size(max = 200)
    @Valid
    private List<@NotNull @Positive Long> orderIds;
}
