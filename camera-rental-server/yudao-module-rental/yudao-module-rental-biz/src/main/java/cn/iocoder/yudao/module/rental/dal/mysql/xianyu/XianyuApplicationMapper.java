package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface XianyuApplicationMapper extends BaseMapperX<XianyuApplicationDO> {

    default XianyuApplicationDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<XianyuApplicationDO>()
                .eq(XianyuApplicationDO::getTenantId, tenantId)
                .eq(XianyuApplicationDO::getId, id));
    }

    default XianyuApplicationDO selectCurrentByTenantId(Long tenantId) {
        XianyuApplicationDO configured = selectOne(new LambdaQueryWrapperX<XianyuApplicationDO>()
                .eq(XianyuApplicationDO::getTenantId, tenantId)
                .isNotNull(XianyuApplicationDO::getAppKey)
                .ne(XianyuApplicationDO::getAppKey, "")
                .orderByAsc(XianyuApplicationDO::getId)
                .last("LIMIT 1"));
        if (configured != null) {
            return configured;
        }
        return selectOne(new LambdaQueryWrapperX<XianyuApplicationDO>()
                .eq(XianyuApplicationDO::getTenantId, tenantId)
                .orderByAsc(XianyuApplicationDO::getId)
                .last("LIMIT 1"));
    }

    default XianyuApplicationDO selectByAppKey(String appKey) {
        return selectOne(new LambdaQueryWrapperX<XianyuApplicationDO>()
                .eq(XianyuApplicationDO::getAppKey, appKey)
                .orderByAsc(XianyuApplicationDO::getId)
                .last("LIMIT 1"));
    }

}
