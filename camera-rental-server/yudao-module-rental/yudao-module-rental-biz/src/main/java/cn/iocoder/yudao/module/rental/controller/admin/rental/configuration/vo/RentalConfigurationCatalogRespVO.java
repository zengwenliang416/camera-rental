package cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 租赁设备目录配置")
@Data
public class RentalConfigurationCatalogRespVO {

    private List<Category> categories;

    @Data
    public static class Category {
        private Long id;
        private String categoryCode;
        private String categoryName;
        private Integer sortOrder;
        private Boolean enabled;
        private Integer lockVersion;
        private List<Model> models;
    }

    @Data
    public static class Model {
        private Long id;
        private Long categoryId;
        private String modelCode;
        private String modelName;
        private String deviceNoPrefix;
        private Integer sortOrder;
        private Boolean enabled;
        private Integer lockVersion;
    }
}
