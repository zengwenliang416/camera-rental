package cn.iocoder.yudao.module.rental.dal.dataobject.rental;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@TableName("rental_staff_inspection_template")
@Data
public class RentalStaffInspectionTemplateDO extends TenantBaseDO {
    @TableId private Long id;
    private String modelCode;
    private String checklistJson;
}
