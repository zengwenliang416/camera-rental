package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * One review record per source and review type keeps replay behavior idempotent.
 */
@Mapper
public interface RentalManualReviewMapper extends BaseMapperX<RentalManualReviewDO> {

    default RentalManualReviewDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalManualReviewDO>()
                .eq(RentalManualReviewDO::getId, id));
    }

    default RentalManualReviewDO selectBySourceAndReviewTypeForUpdate(String sourceType, String sourceIdentifier,
                                                                        String reviewType) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalManualReviewDO>()
                .eq(RentalManualReviewDO::getSourceType, sourceType)
                .eq(RentalManualReviewDO::getSourceIdentifier, sourceIdentifier)
                .eq(RentalManualReviewDO::getReviewType, reviewType));
    }

}
