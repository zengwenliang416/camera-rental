package cn.iocoder.yudao.module.rental.dal.mysql.returnregistration;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationAttachmentDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalReturnRegistrationAttachmentMapper
        extends BaseMapperX<RentalReturnRegistrationAttachmentDO> {

    default RentalReturnRegistrationAttachmentDO selectByRegistrationAndId(
            Long registrationId, Long id) {
        return selectOne(new LambdaQueryWrapper<RentalReturnRegistrationAttachmentDO>()
                .eq(RentalReturnRegistrationAttachmentDO::getRegistrationId, registrationId)
                .eq(RentalReturnRegistrationAttachmentDO::getId, id));
    }

    default List<RentalReturnRegistrationAttachmentDO> selectConfirmedList(Long registrationId) {
        return selectList(new LambdaQueryWrapper<RentalReturnRegistrationAttachmentDO>()
                .eq(RentalReturnRegistrationAttachmentDO::getRegistrationId, registrationId)
                .eq(RentalReturnRegistrationAttachmentDO::getConfirmed, true)
                .orderByAsc(RentalReturnRegistrationAttachmentDO::getCategory)
                .orderByAsc(RentalReturnRegistrationAttachmentDO::getSortNo));
    }

    default long countByCategory(Long registrationId, String category) {
        return selectCount(new LambdaQueryWrapper<RentalReturnRegistrationAttachmentDO>()
                .eq(RentalReturnRegistrationAttachmentDO::getRegistrationId, registrationId)
                .eq(RentalReturnRegistrationAttachmentDO::getCategory, category));
    }

    default long countByRegistration(Long registrationId) {
        return selectCount(new LambdaQueryWrapper<RentalReturnRegistrationAttachmentDO>()
                .eq(RentalReturnRegistrationAttachmentDO::getRegistrationId, registrationId));
    }
}
