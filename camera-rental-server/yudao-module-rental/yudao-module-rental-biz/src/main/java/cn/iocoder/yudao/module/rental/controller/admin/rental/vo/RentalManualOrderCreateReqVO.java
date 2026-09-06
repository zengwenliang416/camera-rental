package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 线下租赁订单手动创建")
@Data
public class RentalManualOrderCreateReqVO {

    @Schema(description = "客户信息")
    @NotNull
    @Valid
    private Customer customer;

    @Schema(description = "订单明细")
    @NotEmpty
    @Valid
    private List<Item> items;

    @Schema(description = "计租开始日期（闭区间）")
    @NotNull
    private LocalDate billableStartDate;

    @Schema(description = "计租结束日期（闭区间）")
    @NotNull
    private LocalDate billableEndDate;

    @Schema(description = "押金，单位分")
    @Min(0)
    private Long depositAmount;

    @Schema(description = "配送信息")
    @NotNull
    @Valid
    private Delivery delivery;

    @Schema(description = "客户信息")
    @Data
    public static class Customer {

        @Schema(description = "客户姓名")
        @NotBlank
        @Size(max = 64)
        private String name;

        @Schema(description = "客户手机号")
        @NotBlank
        @Size(max = 32)
        private String mobile;

        @Schema(description = "微信号")
        @Size(max = 64)
        private String wechatId;

    }

    @Schema(description = "订单明细行")
    @Data
    public static class Item {

        @Schema(description = "设备型号编码")
        @NotBlank
        @Size(max = 128)
        private String modelCode;

        @Schema(description = "数量")
        @NotNull
        @Min(1)
        private Integer quantity;

        @Schema(description = "本明细绑定的具体设备实例 ID")
        @NotEmpty
        @Size(max = 99)
        private List<@NotNull Long> deviceIds;

        @Schema(description = "租金，单位分")
        @NotNull
        @Min(0)
        private Long rentAmount;

    }

    @Schema(description = "配送信息")
    @Data
    public static class Delivery {

        @Schema(description = "配送方式：EXPRESS 快递 / ERRAND 跑腿 / SELF_DELIVERY 自送")
        @NotBlank
        @Size(max = 16)
        private String method;

        @Schema(description = "收货人姓名（跑腿/自送必填）")
        @Size(max = 64)
        private String receiverName;

        @Schema(description = "收货人手机号（跑腿/自送必填）")
        @Size(max = 32)
        private String receiverMobile;

        @Schema(description = "收货地址（跑腿/自送必填）")
        @Size(max = 512)
        private String receiverAddress;

        @Schema(description = "配送备注")
        @Size(max = 255)
        private String remark;

    }

}
