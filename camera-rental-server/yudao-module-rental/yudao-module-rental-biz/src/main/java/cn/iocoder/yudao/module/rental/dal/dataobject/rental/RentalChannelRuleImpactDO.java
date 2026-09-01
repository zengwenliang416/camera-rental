package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RentalChannelRuleImpactDO {

    private Long scannedCount;
    private Long withoutInternalOrderCount;
    private Long mutableInternalOrderCount;
    private Long protectedOrderCount;
    private Long reviewRequiredCount;

}
