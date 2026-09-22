package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

public class RentalStaffWarehouseVO {
    @Data public static class IssueSave {
        @Size(max=128) private String requestKey;
        private Long rentalOrderId;
        private Long deviceId;
        @NotBlank @Size(max=100) private String title;
        @Size(max=1000) private String note;
    }
    @Data public static class IssueAction {
        @NotNull private Long id;
        @NotNull @Min(0) private Integer revision;
        @NotBlank @Pattern(regexp="CLAIM|NOTE|RESOLVE|REOPEN") private String action;
        @Size(max=1000) private String note;
    }
    @Data public static class Issue {
        private Long id; private Long rentalOrderId; private Long deviceId; private String title;
        private String note; private String status; private Long ownerId; private Integer revision; private LocalDateTime createTime;
    }
    @Data public static class StocktakeCreate {
        @NotBlank @Size(max=64) private String warehouseCode;
        @NotBlank @Size(max=100) private String idempotencyKey;
    }
    @Data public static class StocktakeAction {
        @NotNull private Long id; private Long deviceId;
    }
    @Data public static class Stocktake {
        private Long id; private String warehouseCode; private String status;
        private LocalDateTime createTime; private List<StocktakeLine> lines;
    }
    @Data public static class StocktakeLine {
        private Long deviceId; private String deviceNo; private String originalWarehouse;
        private Boolean expected; private Boolean scanned; private Boolean adjusted;
    }
    @Data public static class PhotoAuthorize {
        @NotNull private Long deviceId; @NotNull private Long assignmentId;
        @NotBlank @Pattern(regexp="image/jpeg|image/png") private String contentType;
    }
    public record PhotoUpload(Long id, String uploadUrl) {}
    public record Photo(Long id, String url) {}
    @Data public static class Check {
        @NotBlank @Size(max=60) private String label;
        @NotBlank @Pattern(regexp="PASS|FAIL") private String result;
        @Min(0) @Max(999) private Integer expected;
        @Min(0) @Max(999) private Integer actual;
    }
    @Data public static class TemplateSave {
        @NotBlank @Size(max=64) private String modelCode;
        @NotEmpty @Size(max=30) @Valid private List<Check> checks;
    }
    @Data public static class InspectionSubmit {
        @NotNull private Long deviceId; @NotNull private Long assignmentId;
        @NotBlank @Size(max=100) private String idempotencyKey;
        @NotEmpty @Size(max=30) @Valid private List<Check> checks;
        @Size(max=6) private List<Long> photoIds;
        @Size(max=512) private String note;
    }
    @Data public static class Inspection {
        private Long id; private Long assignmentId; private Long deviceId;
        private String checklistJson; private String photoIdsJson; private Boolean passed;
        private String note; private LocalDateTime createTime;
    }
}
