package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 租赁设备大类及型号目录")
@Data
@AllArgsConstructor
public class RentalDeviceCategoryRespVO {

    private Long id;
    private String categoryCode;
    private String categoryName;
    private List<RentalDeviceModelRespVO> models;

}
