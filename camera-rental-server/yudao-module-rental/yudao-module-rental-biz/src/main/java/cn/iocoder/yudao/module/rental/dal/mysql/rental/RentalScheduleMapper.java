package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RentalScheduleMapper extends BaseMapperX<RentalScheduleDO> {

    default RentalScheduleDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<RentalScheduleDO>()
                .eq(RentalScheduleDO::getId, id));
    }

    @Select("SELECT COUNT(*) FROM rental_schedule"
            + " WHERE tenant_id = #{tenantId} AND device_id = #{deviceId}")
    @InterceptorIgnore(tenantLine = "true")
    long countAllByDeviceId(@Param("tenantId") Long tenantId, @Param("deviceId") Long deviceId);

    /**
     * Locks every effective schedule that overlaps the requested half-open period.
     * The caller must already hold the physical-device row lock before this query.
     */
    default List<RentalScheduleDO> selectEffectiveOverlapsForUpdate(Long deviceId, LocalDate startDate,
                                                                      LocalDate endDateExclusive) {
        return selectList(new LambdaQueryWrapper<RentalScheduleDO>()
                .eq(RentalScheduleDO::getDeviceId, deviceId)
                .eq(RentalScheduleDO::getStatus, "EFFECTIVE")
                .lt(RentalScheduleDO::getOccupyStartDate, endDateExclusive)
                .gt(RentalScheduleDO::getOccupyEndDateExclusive, startDate)
                .last("FOR UPDATE"));
    }

}
