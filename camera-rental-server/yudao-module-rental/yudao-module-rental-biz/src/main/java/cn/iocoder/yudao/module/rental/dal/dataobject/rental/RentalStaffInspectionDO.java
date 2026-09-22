package cn.iocoder.yudao.module.rental.dal.dataobject.rental;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@TableName("rental_staff_inspection")
@Data
public class RentalStaffInspectionDO extends TenantBaseDO {
    @TableId private Long id;
    private Long deviceId;
    private Long assignmentId;
    private String idempotencyKey;
    private String checklistJson;
    private String photoIdsJson;
    private Boolean passed;
    private String note;
}
