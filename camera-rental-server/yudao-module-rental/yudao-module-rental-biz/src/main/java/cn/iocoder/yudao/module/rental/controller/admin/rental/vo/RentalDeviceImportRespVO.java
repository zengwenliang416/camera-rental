package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import lombok.Data;
import java.util.List;

@Data
public class RentalDeviceImportRespVO {
    private String batchId;
    private List<Row> rows;
    private boolean canSubmit;
    @Data
    public static class Row {
        private String fileName;
        private Integer lineNumber;
        private String categoryCode;
        private String equipmentModelCode;
        private String deviceNo;
        private String serialNumber;
        private Long deviceId;
        /** MATCHED, NEW, DUPLICATE, CONFLICT, MISSING */
        private String status;
        /** Stable code, localized by the client. */
        private String reason;
    }
}
