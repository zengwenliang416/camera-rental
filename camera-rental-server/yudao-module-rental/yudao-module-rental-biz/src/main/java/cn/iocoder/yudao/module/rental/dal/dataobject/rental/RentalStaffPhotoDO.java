package cn.iocoder.yudao.module.rental.dal.dataobject.rental;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@TableName("rental_staff_photo")
@Data
public class RentalStaffPhotoDO extends TenantBaseDO {
    @TableId private Long id;
    private Long deviceId;
    private Long assignmentId;
    private Long fileConfigId;
    private String objectPath;
    private Long fileId;
    private Boolean confirmed;
}
