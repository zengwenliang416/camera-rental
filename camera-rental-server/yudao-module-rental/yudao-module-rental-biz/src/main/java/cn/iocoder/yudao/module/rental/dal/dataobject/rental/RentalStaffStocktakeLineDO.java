package cn.iocoder.yudao.module.rental.dal.dataobject.rental;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@TableName("rental_staff_stocktake_line")
@Data
public class RentalStaffStocktakeLineDO extends TenantBaseDO {
    @TableId private Long id;
    private Long stocktakeId;
    private Long deviceId;
    private String deviceNo;
    private String originalWarehouse;
    private Boolean expected;
    private Boolean scanned;
    private Boolean adjusted;
}
