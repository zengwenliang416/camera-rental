package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.util.StringUtils;

@Schema(description = "管理后台 - 闲鱼订单发货 Request VO")
@Data
public class XianyuOrderShipReqVO {

    @NotNull
    private Long channelOrderId;

    private Long deviceId;

    private String deviceNo;

    @NotBlank
    private String idempotencyKey;

    @NotBlank
    private String expressCode;

    @NotBlank
    private String expressName;

    @NotBlank
    @Pattern(regexp = "^\\w{10,}$", message = "快递单号至少 10 位字母或数字")
    private String waybillNo;

    @NotBlank
    @Pattern(regexp = "ADMIN|STAFF", message = "发货来源仅支持 ADMIN 或 STAFF")
    private String source;

    private Boolean ocrConfirmed;

    @Schema(description = "订单仅缺商品型号规则时，是否确认将商品绑定到扫描设备的型号")
    private Boolean bindProductRuleIfMissing;

    @Schema(description = "商品型号已确定但租期不完整时，是否明确确认先发货、后补租期")
    private Boolean allowPendingPlan;

    @AssertTrue(message = "必须传入 deviceId 或 deviceNo")
    public boolean isDevicePresent() {
        return deviceId != null || StringUtils.hasText(deviceNo);
    }

}
