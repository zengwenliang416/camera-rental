package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;
import lombok.Data;
import java.time.LocalDate;
@Data
public class RentalStaffTaskRespVO {
    private String sourceType;
    private Long orderId; private Long deviceId; private Long assignmentId;
    private String orderNo; private String deviceNo; private String equipmentModelCode;
    private LocalDate dueDate;
}
