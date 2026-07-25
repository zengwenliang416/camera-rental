package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for durable channel synchronization outcomes.
 */
@Mapper
public interface XianyuSyncRunMapper extends BaseMapperX<XianyuSyncRunDO> {

}
