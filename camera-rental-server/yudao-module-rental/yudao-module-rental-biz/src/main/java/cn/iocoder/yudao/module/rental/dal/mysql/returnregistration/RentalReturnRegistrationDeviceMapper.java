package cn.iocoder.yudao.module.rental.dal.mysql.returnregistration;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDeviceDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RentalReturnRegistrationDeviceMapper
        extends BaseMapperX<RentalReturnRegistrationDeviceDO> {

    default List<RentalReturnRegistrationDeviceDO> selectListByRegistrationId(Long registrationId) {
        return selectList(new LambdaQueryWrapper<RentalReturnRegistrationDeviceDO>()
                .eq(RentalReturnRegistrationDeviceDO::getRegistrationId, registrationId)
                .orderByAsc(RentalReturnRegistrationDeviceDO::getSortNo));
    }

    default void deleteByRegistrationId(Long registrationId) {
        delete(new LambdaQueryWrapper<RentalReturnRegistrationDeviceDO>()
                .eq(RentalReturnRegistrationDeviceDO::getRegistrationId, registrationId));
    }

    @Select("SELECT COUNT(*) FROM rental_return_registration_device"
            + " WHERE tenant_id = #{tenantId} AND device_id = #{deviceId}")
    @InterceptorIgnore(tenantLine = "true")
    long countAllByDeviceId(@Param("tenantId") Long tenantId, @Param("deviceId") Long deviceId);
}
