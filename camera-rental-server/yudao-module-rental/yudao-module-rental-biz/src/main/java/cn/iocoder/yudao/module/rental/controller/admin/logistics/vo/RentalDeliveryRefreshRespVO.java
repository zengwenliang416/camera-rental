package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 本地物流刷新受理结果")
@Data
public class RentalDeliveryRefreshRespVO {

    private Boolean accepted;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextAllowedAt;
}
