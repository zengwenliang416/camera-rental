package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalHistoricalReconciliationFailureDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalHistoricalReconciliationFailureMapper
        extends BaseMapperX<RentalHistoricalReconciliationFailureDO> {

    default List<RentalHistoricalReconciliationFailureDO> selectListByRunId(Long runId) {
        return selectList(new LambdaQueryWrapperX<RentalHistoricalReconciliationFailureDO>()
                .eq(RentalHistoricalReconciliationFailureDO::getRunId, runId)
                .orderByAsc(RentalHistoricalReconciliationFailureDO::getId));
    }

}

