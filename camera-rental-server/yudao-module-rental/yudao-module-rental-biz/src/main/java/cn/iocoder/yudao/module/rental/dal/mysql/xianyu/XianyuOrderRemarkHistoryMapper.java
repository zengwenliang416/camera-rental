package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderRemarkHistoryDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface XianyuOrderRemarkHistoryMapper extends BaseMapperX<XianyuOrderRemarkHistoryDO> {

    default XianyuOrderRemarkHistoryDO selectLatestEffectiveByOrderIdForUpdate(Long xianyuOrderId) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuOrderRemarkHistoryDO>()
                .eq(XianyuOrderRemarkHistoryDO::getXianyuOrderId, xianyuOrderId)
                .eq(XianyuOrderRemarkHistoryDO::getEffectivePlan, true)
                .orderByDesc(XianyuOrderRemarkHistoryDO::getId)
                .last("LIMIT 1"));
    }

}
