package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface XianyuApplicationMapper extends BaseMapperX<XianyuApplicationDO> {

    default XianyuApplicationDO selectByApplicationCode(String applicationCode) {
        return selectOne(new LambdaQueryWrapperX<XianyuApplicationDO>()
                .eq(XianyuApplicationDO::getApplicationCode, applicationCode));
    }

}
