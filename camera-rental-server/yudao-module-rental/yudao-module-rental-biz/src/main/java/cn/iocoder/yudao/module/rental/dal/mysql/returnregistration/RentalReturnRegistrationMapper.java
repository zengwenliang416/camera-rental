package cn.iocoder.yudao.module.rental.dal.mysql.returnregistration;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface RentalReturnRegistrationMapper extends BaseMapperX<RentalReturnRegistrationDO> {

    default RentalReturnRegistrationDO selectByTokenHash(String tokenHash) {
        return selectOne(new LambdaQueryWrapperX<RentalReturnRegistrationDO>()
                .eq(RentalReturnRegistrationDO::getTokenHash, tokenHash));
    }

    default RentalReturnRegistrationDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapperX<RentalReturnRegistrationDO>()
                .eq(RentalReturnRegistrationDO::getId, id));
    }

    default PageResult<RentalReturnRegistrationDO> selectPage(
            PageParam page, String status, Long orderId, String keyword,
            String serial, LocalDateTime submittedStart, LocalDateTime submittedEnd) {
        return selectPage(page, new LambdaQueryWrapperX<RentalReturnRegistrationDO>()
                .eqIfPresent(RentalReturnRegistrationDO::getStatus, status)
                .eqIfPresent(RentalReturnRegistrationDO::getRentalOrderId, orderId)
                .geIfPresent(RentalReturnRegistrationDO::getSubmittedAt, submittedStart)
                .leIfPresent(RentalReturnRegistrationDO::getSubmittedAt, submittedEnd)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(RentalReturnRegistrationDO::getFormNo, keyword)
                        .or().like(RentalReturnRegistrationDO::getExternalOrderNo, keyword)
                        .or().like(RentalReturnRegistrationDO::getWaybillNo, keyword))
                .apply(serial != null && !serial.isBlank(),
                        "EXISTS (SELECT 1 FROM rental_return_registration_device d"
                                + " WHERE d.registration_id = rental_return_registration.id"
                                + " AND d.tenant_id = rental_return_registration.tenant_id"
                                + " AND d.deleted = b'0'"
                                + " AND d.normalized_serial LIKE CONCAT('%', {0}, '%'))",
                        serial == null ? null : serial.trim().toUpperCase())
                .orderByDesc(RentalReturnRegistrationDO::getId));
    }
}
