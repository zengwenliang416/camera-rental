package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 已发货闲鱼订单补录设备出库 Request VO")
@Data
public class XianyuOrderDispatchBackfillReqVO {

    @NotNull
    private Long channelOrderId;

    private Long deviceId;

    private String deviceNo;

    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    @NotBlank
    @Size(max = 64)
    private String expressCode;

    @NotBlank
    @Size(max = 128)
    private String expressName;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^\\w{10,}$", message = "快递单号至少 10 位字母或数字")
    private String waybillNo;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consignTime;

    @NotBlank
    @Size(max = 480)
    private String reason;

    @AssertTrue(message = "必须传入 deviceId 或 deviceNo")
    public boolean isDevicePresent() {
        return deviceId != null || StringUtils.hasText(deviceNo);
    }

}
